package com.rawskys.microservices.frontend.controllers

import javax.inject.Inject

import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, Controller, Request}

class Dashboard @Inject()(
							 configuration: Configuration,
							 ws: WSClient,
							 val messagesApi: MessagesApi)
		extends Controller {

	val status = Action {
		Ok(Json.obj())
	}

	val port = configuration.underlying.getInt("oauth2.port")
	val accessTokenUrl = ws.url(s"http://localhost:$port/oauth2/access_token")

	val hello = Action { request =>
		request.headers.get("").map { userProfile =>
			Ok(s"Hello, $userProfile")
		}.getOrElse(Redirect(routes.Login.form()))
	}
}
