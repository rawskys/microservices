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
		val redirectUri = request.headers.get("referer").getOrElse(routes.Dashboard.index().url)
		Ok(views.html.login(authUrl, routes.Login.facebook().url + "?redirectUri=" + redirectUri, redirectUri))
	}

	val facebook = Action { request =>
		Ok(views.html.facebook(
			authUrl,
			configuration.getNumber("facebook.clientid").getOrElse(1),
			request.getQueryString("code").getOrElse(""),
			request.getQueryString("redirectUri").getOrElse("")
		))
	}

	def status = Action(Ok(Json.obj("status" -> "ok")))
}
