package com.rawskys.microservices.internaluser.model

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.libs.json._

case class Login(username: String, password: String)

object Login {

	val form = Form(
		mapping(
			"username" -> nonEmptyText,
			"password" -> nonEmptyText(12)
		) {
			(username, password) => Login(username, password)
		} { login =>
			Some(login.username, login.password.toString)
		}
	)
}
