package controllers

import javax.inject.Inject

import model.NewUser
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Registration @Inject()(configuration: Configuration, ws: WSClient, messagesApi: MessagesApi) extends Controller {

	val internalUsersUri = configuration.underlying.getString("internalusers.uri")

	val form = Action {
		Ok(views.html.registration(routes.Login.facebook().url))
	}

	val send = Action.async { implicit request =>
		implicit val messages = messagesApi.preferred(request)
		NewUser.form.bindFromRequest.fold(
			form => Future.successful(BadRequest(form.errorsAsJson)),
			newUser => ws.url(internalUsersUri + "/register")
					.post(Json.obj("user" -> newUser.username, "pass" -> newUser.password, "email" -> newUser.email))
					.map{
						case r if r.statusText == "OK" => Ok(Json.obj())
						case r => BadRequest(Json.obj("error" -> (r.json \ "error").get))
					}
			    .recover {
						case e => InternalServerError(Json.obj("error" -> e.getLocalizedMessage))
					}
		)
	}
}
