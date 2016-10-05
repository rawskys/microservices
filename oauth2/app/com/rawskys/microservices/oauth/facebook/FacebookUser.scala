package com.rawskys.microservices.oauth.facebook

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class FacebookUser(id: String, firstName: String, email: Option[String])

object FacebookUser {

	implicit val facebookUserReads: Reads[FacebookUser] = (
			(JsPath \ "id").read[String] and
					(JsPath \ "first_name").read[String] and
					(JsPath \ "email").readNullable[String]
			) (FacebookUser.apply _)
}

