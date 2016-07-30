package com.rawskys.microservices.internaluser.model

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.libs.json._

case class Login(username: String, password: String)

object Login {

	implicit object LoginReads extends Reads[Login] {

		override def reads(json: JsValue): JsResult[Login] = json match {
			case obj: JsObject => try {
				val username = (obj \ "username").as[String]
				val password = (obj \ "password").as[String]

				JsSuccess(Login(username, password))
			} catch {
				case cause: Throwable => JsError(cause.getMessage)
			}

			case _ => JsError("expected.jsobject")
		}
	}

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
