package com.rawskys.microservices.oauth

import javax.inject.Inject

import play.api.mvc.{Action, Controller}

import scalaoauth2.provider.OAuth2Provider
import scala.concurrent.ExecutionContext.Implicits.global

class OAuth2Controller @Inject()(dataHandler: OAuthDataHandler) extends Controller with OAuth2Provider {

	override val tokenEndpoint = new OAuthTokenEndpoint

	def token = Action.async { implicit request =>
		issueAccessToken(dataHandler)
	}

}
