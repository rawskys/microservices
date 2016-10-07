package com.rawskys.microservices.userprofile.controllers

import javax.inject.Inject

import com.rawskys.microservices.userprofile.model.{FacebookUserProfile, NewUserProfile}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserProfileController @Inject()
(
	configuration: Configuration,
	ws: WSClient,
	val reactiveMongoApi: ReactiveMongoApi,
	val messagesApi: MessagesApi
)
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
					case r => Ok(Json.obj("id" -> newUserProfile.id))
				} recover {
					case e =>
						Logger.error("could not create a user profile", e)
						InternalServerError(Json.obj("error" -> e.getMessage))
				}
			}
		)
	}

	def read(id: String) = Action.async { implicit request =>
		collection.find(BSONDocument("_id" -> BSONObjectID(id))).one
				.map {
					case Some(profile) => Ok(Json.obj("name" -> profile.getAs[String]("name")))
					case e => BadRequest(Json.obj("error" -> "not found"))
				}
				.recover {
					case e => InternalServerError(Json.obj("error" -> e.getLocalizedMessage))
				}
	}

	def updateFacebookUserProfile(facebookUserId: Long) = Action.async { implicit request =>
		implicit val messages = messagesApi.preferred(request)
		FacebookUserProfile.form.bindFromRequest.fold(
			errors => Future.successful(BadRequest(Json.obj("error" -> errors.errorsAsJson))),
			facebookUserProfile => {
				collection.update(
					BSONDocument("facebookId" -> facebookUserId),
					BSONDocument("$set" -> facebookUserProfile)
				) map {
					case r if r.hasErrors => BadRequest(Json.obj("error" -> r.message))
					case r => Ok(Json.obj("id" -> r.originalDocument.get.getAs[String]("_id")))
				} recover {
					case e => InternalServerError(Json.obj("error" -> e.getMessage))
				}
			}
		)
	}

	def readFacebookUserProfile(facebookUserId: Long) = Action.async { implicit request =>
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
