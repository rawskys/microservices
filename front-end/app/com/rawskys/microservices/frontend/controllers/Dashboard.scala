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

	val hello = Action { request =>
		request.session.get("userProfile").map { userProfile =>
			Ok(s"Hello, $userProfile")
		}.getOrElse(Redirect(routes.Login.form()))
	}
//	val port = configuration.underlying.getInt("userprofile.port")
//	val createUserProfileRequest = ws.url(s"http://localhost:$port/create")
}
