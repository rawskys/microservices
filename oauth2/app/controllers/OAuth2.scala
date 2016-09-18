package controllers

import javax.inject.Inject

import com.rawskys.microservices.oauth.{OAuthDataHandler, OAuthTokenEndpoint}
import io.netty.handler.codec.http.HttpMethod
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scalaoauth2.provider.{OAuth2Provider, OAuth2ProviderActionBuilders}

class OAuth2 @Inject()(dataHandler: OAuthDataHandler) extends Controller with OAuth2Provider {

	override val tokenEndpoint = new OAuthTokenEndpoint

	def token = Action.async { implicit request =>
		issueAccessToken(dataHandler).map(_.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> "*"))
	}

	def user = OAuth2ProviderActionBuilders.AuthorizedAction(dataHandler) { request =>
		Ok(Json.obj("user" -> request.authInfo.user.username))
	}

	def allow = Action(
		Ok.withHeaders(
//			ACCESS_CONTROL_ALLOW_METHODS -> HttpMethod.POST.toString,
//			ACCESS_CONTROL_MAX_AGE -> "86400",
//			ACCESS_CONTROL_ALLOW_HEADERS -> CACHE_CONTROL,
//			ACCESS_CONTROL_ALLOW_ORIGIN -> "http://localhost:9000"
		)
	)
}