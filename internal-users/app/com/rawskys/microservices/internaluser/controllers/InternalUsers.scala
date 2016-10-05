package com.rawskys.microservices.internaluser.controllers

import javax.inject.Inject

import com.rawskys.microservices.internaluser.model.{Login, NewUser}
import org.mindrot.jbcrypt.BCrypt
import play.api.{Configuration, Logger}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class InternalUsers @Inject()
(
	configuration: Configuration,
	ws: WSClient,
	val reactiveMongoApi: ReactiveMongoApi,
	val messagesApi: MessagesApi
) extends Controller with MongoController with ReactiveMongoComponents {

	val collection = db.collection[BSONCollection]("user")
	val userprofileUri = configuration.underlying.getString("userprofile.uri")

	val status = Action.async {
		collection.count().map {
			users =>
				Ok(Json.obj("users" -> users))
		}.recover {
			case e => BadRequest(Json.obj("error" -> e.getLocalizedMessage))
		}
	}

	def register = Action.async {
		implicit request =>
			implicit val messages = messagesApi.preferred(request)
			NewUser.form.bindFromRequest.fold(
				errors => Future.successful(BadRequest(Json.obj("error" -> errors.errorsAsJson))),
				newUser => ws.url(s"$userprofileUri/create")
					.withHeaders("Accept" -> "application/json")
					.withRequestTimeout(10000.millis)
					.post(Json.obj("_id" -> newUser.id.get, "name" -> newUser.name, "email" -> newUser.email))
					.flatMap {
						case r if r.status != 200 => Future.successful(BadRequest(Json.obj("error" -> r.json)))
						case _ => collection.insert(newUser)
							.map(_ => Ok(Json.obj("status" -> "added")))
							.recover {
								case e => BadRequest(Json.obj("error" -> e.getLocalizedMessage))
							}
					}
					.recover {
						case e => BadRequest(Json.obj("error" -> e.getLocalizedMessage))
					}
			)
	}

	def verify = Action.async {
		implicit request =>
			implicit val messages = messagesApi.preferred(request)
			Login.form.bindFromRequest.fold(
				errors => Future.successful(BadRequest(Json.obj("error" -> errors.errorsAsJson))),
				login => collection.find(BSONDocument("user" -> login.username)).one
					.map {
						case Some(user) if BCrypt.checkpw(login.password, user.getAs[String]("pass").get) =>
							Ok(Json.obj("userId" -> user.getAs[BSONObjectID]("_id").get.stringify))
						case _ => BadRequest(Json.obj("error" -> "notFound"))
					}
					.recover {
						case e => InternalServerError(Json.obj("error" -> e.getLocalizedMessage))
					}
			)
	}
}
