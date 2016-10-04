package com.rawskys.microservices.userprofile.controllers

import javax.inject.Inject

import com.rawskys.microservices.userprofile.model.NewUserProfile
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserProfileController @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
		extends Controller with MongoController with ReactiveMongoComponents {

	val collection = db.collection[BSONCollection]("userprofile")

	def status = Action.async {
		collection.count().map { profiles =>
			Ok(Json.obj("profiles" -> profiles))
		}.recover {
			case e => BadRequest(Json.obj("error" -> e.getMessage))
		}
	}

	def create = Action.async { implicit request =>
		implicit val messages = messagesApi.preferred(request)
		NewUserProfile.form.bindFromRequest.fold(
			errors => Future.successful(BadRequest(Json.obj("error" -> errors.errorsAsJson))),
			newUserProfile => {
				collection.insert(newUserProfile).map {
					case result if result.hasErrors => BadRequest(Json.obj("error" -> result.message))
					case _ => Ok(Json.obj("status" -> "added"))
				}.recover { case e => BadRequest(Json.obj("error" -> e.getMessage)) }
			}
		)
	}

	def readFacebookUserProfile(facebookUserId: String) = Action.async { implicit request =>
		collection.find(BSONDocument("facebookId" -> facebookUserId)).one
				.map {
					case Some(profile) => Ok(Json.obj("id" -> profile.getAs[BSONObjectID]("_id").get.stringify))
					case _ => BadRequest(Json.obj("error" -> "notFound"))
				}
				.recover {
					case e => InternalServerError(Json.obj("error" -> e.getLocalizedMessage))
				}
	}
}
