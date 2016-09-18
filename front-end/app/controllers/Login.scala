package controllers

import javax.inject.Inject

import play.api.{Configuration, Logger}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, Controller, Request}

class Login @Inject()(configuration: Configuration, ws: WSClient) extends Controller {

	val authPort = configuration.underlying.getNumber("oauth.port")
	val authUrl = s"http://localhost:$authPort/token"
	def redirectUri(implicit request: Request[AnyContent]) = s"http://${request.host}${request.path}"

	val facebookClientId = 1 //configuration.underlying.getNumber("facebook.clientid")
	def facebookAuthUrl(implicit request: Request[AnyContent]) = {
		s"https://www.facebook.com/dialog/oauth?client_id=$facebookClientId&redirect_uri=$redirectUri"
	}

	def form(): Action[AnyContent] = form(routes.Dashboard.index().url)

	def form(redirectUrl: String) = Action { implicit request =>
		Unauthorized(views.html.login(authUrl, facebookAuthUrl, redirectUrl))
	}

	def status = Action(Ok(Json.obj("status" -> "ok")))
}
