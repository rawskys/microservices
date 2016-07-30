package com.rawskys.microservices.userprofile.model

import play.api.libs.json._

case class UserProfile(firstName: String, email: String)

object UserProfile {

	implicit object UserProfileWrites extends OWrites[UserProfile] {

		override def writes(userProfile: UserProfile): JsObject = Json.obj(
			"firstName" -> userProfile.firstName,
			"email" -> userProfile.email
		)
	}
}
