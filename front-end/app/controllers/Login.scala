package controllers

import javax.inject.Inject

import play.api.{Configuration, Logger}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, Controller, Request}

class Login @Inject()(configuration: Configuration, ws: WSClient) extends Controller {

	val authPort = configuration.underlying.getNumber("oauth.port")
	val authUrl = s"http://localhost:$authPort/token"

	val form = Action { implicit request =>
		val facebookCode: Option[String] = request.getQueryString("code")
		Ok(views.html.login(
			authUrl,
			configuration.getNumber("facebook.clientid").getOrElse(1),
			facebookCode.map(_ => routes.Login.form().absoluteURL)
				.getOrElse(request.headers.get("referer").getOrElse(routes.Dashboard.index().absoluteURL)),
			facebookCode.getOrElse(""),
			facebookCode.map(_ => "authorization_code").getOrElse("password")
		))
	}

	def status = Action(Ok(Json.obj("status" -> "ok")))
}
