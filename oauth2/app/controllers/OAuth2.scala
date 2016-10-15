package controllers

import javax.inject.Inject

import com.rawskys.microservices.oauth.{OAuthDataHandler, OAuthTokenEndpoint}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaoauth2.provider.OAuth2Provider

class OAuth2 @Inject()(dataHandler: OAuthDataHandler) extends Controller with OAuth2Provider {

	override val tokenEndpoint = new OAuthTokenEndpoint

	def token = Action.async { implicit request =>
		issueAccessToken(dataHandler).map(_.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*"))
	}

	def account = Action.async { implicit request =>
		authorize(dataHandler) { authInfo =>
			Future.successful(Ok(Json.obj("id" -> authInfo.user.id)).withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*"))
		}.map(_.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*"))

	def accountOptions = Action { request =>
		Ok(Json.obj()).withHeaders(
			ACCESS_CONTROL_ALLOW_ORIGIN -> "*",
			ACCESS_CONTROL_ALLOW_HEADERS -> "authorization"
		)
	}

	def status = Action { request =>
		Ok(Json.obj())
	}
}
