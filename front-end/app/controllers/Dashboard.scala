package controllers

import javax.inject.Inject

import play.api.{Configuration, Logger}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Dashboard @Inject()(
							 configuration: Configuration,
							 ws: WSClient,
							 val messagesApi: MessagesApi)
		extends Controller {

	val status = Action {
		Ok(Json.obj())
	}

	val index = Action {
		Ok(views.html.dashboard())
	}

	val authPort = configuration.underlying.getNumber("oauth.port")

	val oauthUserUrl = s"http://localhost:$authPort/user"
	val oauthRefreshTokenUrl = s"http://localhost:$authPort/token"

	val profile = Action.async { implicit request =>
		request.headers.get("Authorization").map(token =>
			ws.url(oauthUserUrl)
					.withHeaders("Authorization" -> token)
					.get()
					.map {
						case r if r.statusText == "OK" => Ok(Json.obj("user" -> r.json))
						case r =>
							BadRequest(Json.obj("error" -> "invalid token", "refreshTokenUri" -> oauthRefreshTokenUrl))
					}
					.recover {
						case e =>
							InternalServerError(Json.obj("error" -> e.getMessage))
					}
		).getOrElse(Future.successful(BadRequest(Json.obj("error" -> "no token"))))
	}
}
