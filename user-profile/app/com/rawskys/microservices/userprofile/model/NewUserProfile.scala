package com.rawskys.microservices.userprofile.model

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints.{emailAddress, pattern}
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter, BSONObjectID}

case class NewUserProfile(id: Option[String], name: String, email: Option[String], facebookId: Option[Long])

object NewUserProfile {

	implicit object NewUserProfileWriter extends BSONDocumentWriter[NewUserProfile] {

		override def write(newUserProfile: NewUserProfile): BSONDocument = {
			BSONDocument(
				"_id" -> newUserProfile.id.map(BSONObjectID(_)),
				"name" -> newUserProfile.name,
				"email" -> newUserProfile.email,
				"facebookId" -> newUserProfile.facebookId
			)
		}
	}

	val form = Form(
		mapping(
			"_id" -> optional(text verifying pattern("""[a-fA-F0-9]{24}""".r, error = "error.objectId")),
			"name" -> nonEmptyText,
			"email" -> optional(text verifying emailAddress),
			"facebookId" -> optional(longNumber)
		)(NewUserProfile.apply)(NewUserProfile.unapply)
	)
}
