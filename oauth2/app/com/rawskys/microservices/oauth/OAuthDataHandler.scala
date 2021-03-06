package com.rawskys.microservices.oauth

import javax.inject.Inject

import com.rawskys.microservices.oauth.facebook.FacebookUser
import org.joda.time.DateTime
import org.sedis.Pool
import play.api.{Configuration, Logger}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, Json, _}
import play.api.libs.ws.WSClient

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaoauth2.provider._

class OAuthDataHandler @Inject()(ws: WSClient, sedisPool: Pool, config: Configuration)
	extends DataHandler[AccountInfo] {

	val accessTokenExpire = Some(config.getMilliseconds("oauth2.tokenExpire").getOrElse(60 * 60L * 1000) / 1000)

	val bearerTokenGenerator = new BearerTokenGenerator

	def validateClient(request: AuthorizationRequest): Future[Boolean] = {
		// TODO validate client
		Future.successful(true)
	}

	val internalUsersUri = config.underlying.getString("internalusers.uri")
	val verifyUserRequest = ws.url(s"$internalUsersUri/verify")

	val facebookClientId = config.underlying.getString("facebook.clientid")

	val facebookRedirectUri = config.underlying.getString("facebook.redirecturi")

	val facebookClientSecret = config.underlying.getString("facebook.secret")

	val userProfileUri = config.underlying.getString("userprofile.uri")
	val facebookUserProfileUrl = (id: String) => ws.url(s"$userProfileUri/facebookUserProfile/$id")
	val createUserProfileUrl = ws.url(s"$userProfileUri/create")

	def findUser(request: AuthorizationRequest): Future[Option[AccountInfo]] = {
		val passwordRequest = PasswordRequest(request)
		verifyUserRequest.withHeaders("Accept" -> "application/json")
			.withRequestTimeout(10000.millis)
			.post(Json.obj("username" -> passwordRequest.username, "password" -> passwordRequest.password))
			.map {
				case r if r.statusText == "OK" =>
					Some(AccountInfo((r.json \ "userId").as[String]))
				case r =>
					None
			}
			.recover {
				case e =>
					Logger.error("verify user request error", e)
					None
			}
	}

	def createAccessToken(authInfo: AuthInfo[AccountInfo]): Future[AccessToken] = {
		val refreshToken = Some(bearerTokenGenerator.generateSHAToken(authInfo.user.id))
		val accessToken = bearerTokenGenerator.generateSHAToken(authInfo.user.id)
		val now = DateTime.now().toDate

		val tokenObject = AccessToken(accessToken, refreshToken, authInfo.scope, accessTokenExpire, now)
		saveToken(authInfo, tokenObject)

		Future.successful(tokenObject)
	}

	private def saveToken(authInfo: AuthInfo[AccountInfo], tokenObject: AccessToken) = sedisPool.withClient { w =>
		val userId = authInfo.user.id
		val clientId = authInfo.clientId.get

		for (existing <- getAccessToken(userId, clientId)) {
			w.del(s"oauth:refresh_token:${existing.refreshToken.get}")
			w.del(s"oauth:access_token:${existing.token}")
		}

		w.set(key(userId, clientId), Json.stringify(Json.toJson(tokenObject)))
		w.expire(key(userId, clientId), tokenObject.expiresIn.get.toInt)

		w.set(s"oauth:refresh_token:${tokenObject.refreshToken.get}", Json.stringify(Json.toJson(authInfo)))
		w.expire(s"oauth:refresh_token:${tokenObject.refreshToken.get}", 100 * tokenObject.expiresIn.get.toInt)

		w.set(s"oauth:access_token:${tokenObject.token}", Json.stringify(Json.toJson(authInfo)))
		w.expire(s"oauth:access_token:${tokenObject.token}", tokenObject.expiresIn.get.toInt)
	}

	def key(userId: String, clientId: String) = s"oauth:$userId:$clientId"

	def getStoredAccessToken(authInfo: AuthInfo[AccountInfo]): Future[Option[AccessToken]] = {
		Future.successful(getAccessToken(authInfo.user.id, authInfo.clientId.get) match {
			case Some(token) if token.scope.equals(authInfo.scope) => Some(token)
			case _ => None // no previous token or scope changed
		})
	}

	private def getAccessToken(userId: String, clientId: String): Option[AccessToken] = sedisPool.withClient { w =>
		w.get(key(userId, clientId)).flatMap(Json.parse(_).validate[AccessToken].asOpt)
	}

	def refreshAccessToken(authInfo: AuthInfo[AccountInfo], refreshToken: String): Future[AccessToken] = {
		Future.successful(getAccessToken(authInfo.user.id, authInfo.clientId.get).getOrElse {
			val accessToken = bearerTokenGenerator.generateSHAToken(authInfo.user.id)
			val now = DateTime.now().toDate
			val tokenObject = AccessToken(accessToken, Some(refreshToken), authInfo.scope, accessTokenExpire, now)
			saveToken(authInfo, tokenObject)
			tokenObject
		})
	}

	def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[AccountInfo]]] = {
		Future.successful(sedisPool.withClient { w =>
			w.get(s"oauth:access_token:${accessToken.token}")
				.flatMap(Json.parse(_).validate[AuthInfo[AccountInfo]].asOpt)
		})
	}

	def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[AccountInfo]]] = {
		Future.successful(sedisPool.withClient { w =>
			w.get(s"oauth:refresh_token:$refreshToken")
				.flatMap(Json.parse(_).validate[AuthInfo[AccountInfo]].asOpt)
		})
	}

	def findAuthInfoByCode(code: String): Future[Option[AuthInfo[AccountInfo]]] = {
		ws.url(s"https://graph.facebook.com/v2.7/oauth/access_token")
			.withQueryString(
				"client_id" -> facebookClientId,
				"redirect_uri" -> facebookRedirectUri,
				"client_secret" -> facebookClientSecret,
				"code" -> code
			)
			.get()
			.flatMap {
				case r if r.statusText == "OK" =>
					val facebookAccessToken = (r.json \ "access_token").as[String]
					ws.url(
						s"https://graph.facebook.com/v2.7/me?access_token=$facebookAccessToken&fields=first_name,email")
						.get()
						.flatMap {
							case r => findFacebookUserProfile(r.json.validate[FacebookUser].get)
						}
						.recover {
							case e =>
								Logger.error("facebook user data request error", e)
								None
						}
				case r => Future.successful(None)
			}
			.recover {
				case e =>
					Logger.error("facebook access token request error", e)
					None
			}
	}

	def findFacebookUserProfile(fbUser: FacebookUser): Future[Option[AuthInfo[AccountInfo]]] = {
		facebookUserProfileUrl(fbUser.id).get()
			.flatMap {
				case r if r.statusText == "OK" =>
					facebookUserProfileUrl(fbUser.id).put(Json.obj("name" -> fbUser.firstName, "email" -> fbUser.email))
						.map {
							case _ =>
								Some(
									AuthInfo(AccountInfo((r.json \ "id").as[String]), Some("frontend"), None, None)
								)
						}
						.recover {
							case e =>
								Logger.error("update facebook user profile request error", e)
								None
						}
				case _ =>
					createUserProfileUrl
						.post(Json.obj("name" -> fbUser.firstName, "email" -> fbUser.email, "facebookId" -> fbUser.id))
						.flatMap {
							case _ => facebookUserProfileUrl(fbUser.id).get()
								.map {
									case r => Some(
										AuthInfo(AccountInfo((r.json \ "id").as[String]), Some("frontend"), None, None)
									)
								}
								.recover {
									case e =>
										Logger.error("read facebook user profile request error", e)
										None
								}
						}
						.recover {
							case e =>
								Logger.error("create user profile request error", e)
								None
						}
			}
			.recover {
				case e =>
					Logger.error("read facebook user profile request error", e)
					None
			}
	}

	def deleteAuthCode(code: String): Future[Unit] = {
		Future.successful(())
	}

	def findAccessToken(token: String): Future[Option[AccessToken]] = {
		Future.successful(sedisPool.withClient { w =>
			w.get(s"oauth:access_token:$token")
				.flatMap(Json.parse(_).validate[AuthInfo[AccountInfo]].asOpt)
				.flatMap(authInfo => w.get(key(authInfo.user.id, authInfo.clientId.get)))
				.flatMap(Json.parse(_).validate[AccessToken].asOpt)
		})
	}

	implicit val tokenFormat = Json.format[AccessToken]

	implicit val accountInfoFormat = Json.format[AccountInfo]

	implicit val authInfoFormat: Format[AuthInfo[AccountInfo]] =
		((__ \ "user").format[AccountInfo] ~
			(__ \ "clientId").formatNullable[String] ~
			(__ \ "scope").formatNullable[String] ~
			(__ \ "redirectUri").formatNullable[String]) (AuthInfo.apply, unlift(AuthInfo.unapply))
}
