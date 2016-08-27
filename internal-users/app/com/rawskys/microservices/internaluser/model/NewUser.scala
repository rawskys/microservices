package com.rawskys.microservices.internaluser.model

import org.mindrot.jbcrypt.BCrypt
import play.api.data.Forms.{mapping, nonEmptyText, optional, text}
import play.api.data._
import play.api.data.validation.Constraints.{emailAddress, pattern}
import play.api.libs.json._
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter, BSONObjectID}

case class NewUser(id: Option[String], name: String, pass: String, email: String)

object NewUser {

	val form = Form(
		mapping(
			"_id" -> optional(text verifying pattern(
				"""[a-fA-F0-9]{24}""".r, error = "error.objectId")),
			"user" -> nonEmptyText,
			"pass" -> nonEmptyText(12),
			"email" -> (text verifying emailAddress)
		)(NewUser.apply)(NewUser.unapply)
	)

	implicit object NewUserWriter extends BSONDocumentWriter[NewUser] {

		override def write(newUser: NewUser): BSONDocument = {
			BSONDocument(
				"_id" -> newUser.id.map(s => BSONObjectID(s)).getOrElse(BSONObjectID.generate),
				"user" -> newUser.name,
				"pass" -> BCrypt.hashpw(newUser.pass, BCrypt.gensalt())
			)
		}
	}

}
