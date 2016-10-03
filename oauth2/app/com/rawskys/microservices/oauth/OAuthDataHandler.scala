package com.rawskys.microservices.oauth

import javax.inject.Inject

import org.joda.time.DateTime
import org.sedis.Pool
import play.api.{Configuration, Logger}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, Json, _}
import play.api.libs.ws.{WSClient, WSResponse}

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

	val internalUsersPort = config.underlying.getNumber("internalusers.port")
	val verifyUserRequest = ws.url(s"http://localhost:$internalUsersPort/verify")

	def findUser(request: AuthorizationRequest): Future[Option[AccountInfo]] = {
		val passwordRequest = PasswordRequest(request)
		verifyUserRequest.withHeaders("Accept" -> "application/json")
			.withRequestTimeout(10000.millis)
			.post(Json.obj("username" -> passwordRequest.username, "password" -> passwordRequest.password))
			.map {
				case r: WSResponse if r.statusText == "OK" && r.json == Json.obj("verified" -> true) =>
					Some(AccountInfo("1", request.param("username").get))
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
		val username = authInfo.user.username
		val clientId = authInfo.clientId.get

		for (existing <- getAccessToken(username, clientId)) {
			w.del(s"oauth:refresh_token:${existing.refreshToken.get}")
			w.del(s"oauth:access_token:${existing.token}")
		}

		w.set(key(username, clientId), Json.stringify(Json.toJson(tokenObject)))
		w.expire(key(username, clientId), tokenObject.expiresIn.get.toInt)

		w.set(s"oauth:refresh_token:${tokenObject.refreshToken.get}", Json.stringify(Json.toJson(authInfo)))
		w.expire(s"oauth:refresh_token:${tokenObject.refreshToken.get}", 100 * tokenObject.expiresIn.get.toInt)

		w.set(s"oauth:access_token:${tokenObject.token}", Json.stringify(Json.toJson(authInfo)))
		w.expire(s"oauth:access_token:${tokenObject.token}", tokenObject.expiresIn.get.toInt)
	}

	def key(username: String, clientId: String) = s"oauth:$username:$clientId"

	def getStoredAccessToken(authInfo: AuthInfo[AccountInfo]): Future[Option[AccessToken]] = {
		Future.successful(getAccessToken(authInfo.user.username, authInfo.clientId.get) match {
			case Some(token) if token.scope.equals(authInfo.scope) => Some(token)
			case _ => None // no previous token or scope changed
		})
	}

	private def getAccessToken(username: String, clientId: String): Option[AccessToken] = sedisPool.withClient { w =>
		w.get(key(username, clientId)).flatMap(Json.parse(_).validate[AccessToken].asOpt)
	}

	def refreshAccessToken(authInfo: AuthInfo[AccountInfo], refreshToken: String): Future[AccessToken] = {
		Future.successful(getAccessToken(authInfo.user.username, authInfo.clientId.get).getOrElse {
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
				"client_id" -> config.underlying.getString("facebook.clientid"),
				"redirect_uri" -> "http://localhost:9000/facebook",
				"client_secret" -> config.underlying.getString("facebook.secret"),
				"code" -> code
			)
			.get()
			.flatMap {
				case r if r.statusText == "OK" =>
					val facebookAccessToken = r.json.as((JsPath \ "access_token").read[String])
					ws.url(s"https://graph.facebook.com/v2.7/me?access_token=$facebookAccessToken&fields=first_name")
						.get()
						.map {
							case r =>
								Some(AuthInfo(
									AccountInfo(
										r.json.as((JsPath \ "id").read[String]),
										r.json.as((JsPath \ "first_name").read[String])
									),
									Some("frontend"),
									None,
									None
								))
						}
						.recover {
							case e =>
								Logger.error("facebook user data request error", e)
								None
						}
				case r =>
					Logger.debug(r.json.toString())
					Future.successful(None)
			}
			.recover {
				case e =>
					Logger.error("facebook access token request error", e)
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
				.flatMap(authInfo => w.get(key(authInfo.user.username, authInfo.clientId.get)))
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
