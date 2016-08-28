package com.rawskys.microservices.frontend.controllers

import javax.inject.Inject

import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, Controller, Request}

import scala.concurrent.ExecutionContext.Implicits.global

class Login @Inject()(configuration: Configuration, ws: WSClient) extends Controller {

	val facebookClientId = configuration.underlying.getNumber("facebook.clientid")

	def facebookAuthUrl(request: Request[AnyContent]) = {
		val redirectUri = "http://" + request.host + request.path
		s"https://www.facebook.com/dialog/oauth?client_id=$facebookClientId&redirect_uri=$redirectUri"
	}

	val form = Action { request =>
		Unauthorized(views.html.login(facebookAuthUrl(request)))
	}

	val port = configuration.underlying.getNumber("internalusers.port")

	val verifyUserRequest = ws.url(s"http://localhost:$port/verify")

	def verify = Action.async { request =>
		verifyUserRequest.withHeaders("Accept" -> "application/json")
				.withRequestTimeout(10000)
				.post(request.body.asJson.get)
				.map {
					case r if r.status != 200 => BadRequest(Json.obj("error" -> r.json))
					case r => {
						Ok(Json.obj("verification" -> r.json))
					}
				}.recover {
			case e => BadRequest(Json.obj("error" -> e.getLocalizedMessage))
		}
	}
}
