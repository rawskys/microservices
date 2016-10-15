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

	val authUri = configuration.underlying.getString("oauth.uri")

	val oauthUserUrl = s"$authUri/user"

	val oauthRefreshTokenUrl = s"$authUri/token"

	val index = Action {
		Ok(views.html.dashboard(authUri + "/account", oauthRefreshTokenUrl))
	}

	val userProfileUri = configuration.underlying.getString("userprofile.uri")

	def readProfile(id: String) = Action.async { request =>
		request.headers.get("Authorization").map(token =>
			ws.url(userProfileUri + "/profile/" + id)
					.withHeaders("Authorization" -> token)
					.get()
					.map {
						case r if r.statusText == "OK" => Ok(Json.obj("name" -> (r.json \ "name").as[String]))
					}
					.recover {
						case e =>
							InternalServerError(Json.obj("error" -> e.getMessage))
					}
		).getOrElse(Future.successful(BadRequest(Json.obj("error" -> "no token"))))
	}
}
