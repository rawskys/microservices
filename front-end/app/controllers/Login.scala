package controllers

import javax.inject.Inject

import play.api.{Configuration, Logger}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, Controller, Request}

class Login @Inject()(configuration: Configuration, ws: WSClient) extends Controller {

	val authPort = configuration.underlying.getNumber("oauth.port")
	val authUrl = s"http://localhost:$authPort/token"

	def facebookAuthUrl(redirectUri: String) = {
		val facebookClientId = configuration.getNumber("facebook.clientid").getOrElse(1)
		s"https://www.facebook.com/dialog/oauth?client_id=$facebookClientId&redirect_uri=$redirectUri"
	}

	val form = Action { implicit request =>
		val redirectUri = request.headers.get("referer").getOrElse(routes.Dashboard.index().absoluteURL)
		Ok(views.html.login(authUrl, facebookAuthUrl(redirectUri), redirectUri))
	}

	def status = Action(Ok(Json.obj("status" -> "ok")))
}
