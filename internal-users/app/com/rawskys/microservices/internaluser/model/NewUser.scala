package com.rawskys.microservices.internaluser.model

import org.mindrot.jbcrypt.BCrypt
import play.api.data.Forms.{mapping, nonEmptyText, optional, text}
import play.api.data._
import play.api.data.validation.Constraints.{emailAddress, pattern}
import play.api.libs.json._
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter, BSONObjectID}

case class NewUser(name: String, pass: String, email: String) {
	lazy val id = BSONObjectID.generate()
}

object NewUser {

	val form = Form(
		mapping(
			"name" -> nonEmptyText,
			"pass" -> nonEmptyText(12),
			"email" -> (text verifying emailAddress)
		)(NewUser.apply)(NewUser.unapply)
	)

	implicit object NewUserWriter extends BSONDocumentWriter[NewUser] {

		override def write(newUser: NewUser): BSONDocument = {
			BSONDocument(
				"_id" -> newUser.id,
				"name" -> newUser.email,
				"pass" -> BCrypt.hashpw(newUser.pass, BCrypt.gensalt())
			)
		}
	}

}
