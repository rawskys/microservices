package com.rawskys.microservices.userprofile.model

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints.emailAddress
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter}

case class FacebookUserProfile(name: String, email: Option[String])

object FacebookUserProfile {

	implicit object FacebookUserProfileWriter extends BSONDocumentWriter[FacebookUserProfile] {

		override def write(facebookUserProfile: FacebookUserProfile): BSONDocument = {
			BSONDocument(
				"name" -> facebookUserProfile.name,
				"email" -> facebookUserProfile.email
			)
		}
	}

	val form = Form(
		mapping(
			"name" -> nonEmptyText,
			"email" -> optional(text verifying emailAddress)
		)(FacebookUserProfile.apply)(FacebookUserProfile.unapply)
	)
}
