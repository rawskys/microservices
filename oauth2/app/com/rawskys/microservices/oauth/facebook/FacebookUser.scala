package com.rawskys.microservices.oauth.facebook

import play.api.libs.json.JsPath
import play.api.libs.functional.syntax._

case class FacebookUser(id: String, firstName: String, email: Option[String]) {

}

object FacebookUser {

	implicit val facebookUserReads = (
			(JsPath \ "id").read[String] and
					(JsPath \ "first_name").read[String] and
					(JsPath \ "email").readNullable[String]
			) (FacebookUser.apply _)
}

