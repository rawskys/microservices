package com.rawskys.microservices.userprofile.controllers

import javax.inject.Inject

import com.rawskys.microservices.userprofile.model.NewUserProfile
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserProfileController @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
		extends Controller with MongoController with ReactiveMongoComponents {

	val collection = db.collection[BSONCollection]("userprofile")

	def create = Action.async { implicit request =>
		implicit val messages = messagesApi.preferred(request)
		NewUserProfile.form.bindFromRequest.fold(
			errors => Future.successful(BadRequest(Json.obj("error" -> errors.errorsAsJson))),
			newUserProfile => {
				println(newUserProfile)
				collection.insert(newUserProfile).map {
					case result if result.hasErrors => BadRequest(Json.obj("error" -> result.message))
					case _ => Ok(Json.obj("status" -> "added"))
				}.recover { case e => BadRequest(Json.obj("error" -> e.getMessage)) }
			}
		)
	}

}
