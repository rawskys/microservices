package com.rawskys.microservices.userprofile.model

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, text}
import play.api.data.validation.Constraints.{emailAddress, pattern}
import play.api.libs.json._
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter, BSONObjectID}

case class NewUserProfile(id: String, name: String, email: String)

object NewUserProfile {

	implicit object NewUserProfileWriter extends BSONDocumentWriter[NewUserProfile] {

		override def write(newUserProfile: NewUserProfile): BSONDocument = {
			BSONDocument(
				"_id" -> BSONObjectID(newUserProfile.id),
				"name" -> newUserProfile.name,
				"email" -> newUserProfile.email
			)
		}
	}

	val form = Form(
		mapping(
			"_id" -> (text verifying pattern("""[a-fA-F0-9]{24}""".r, error = "error.objectId")),
			"name" -> nonEmptyText,
			"email" -> (text verifying emailAddress)
		)(NewUserProfile.apply)(NewUserProfile.unapply)
	)
}
