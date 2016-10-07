package model

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints.{emailAddress, pattern}

case class NewUser(firstName: String, email: String, password: String)

object NewUser {

	val form = Form(
		mapping(
			"firstName" -> nonEmptyText,
			"email" -> (nonEmptyText verifying emailAddress),
			"password" -> nonEmptyText
		)(NewUser.apply)(NewUser.unapply)
	)
}
