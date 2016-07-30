package com.rawskys.microservices.internaluser.controllers

import javax.inject.Inject

import com.rawskys.microservices.internaluser.model.{Login, NewUser}
import org.mindrot.jbcrypt.BCrypt
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InternalUsers @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
		extends Controller with MongoController with ReactiveMongoComponents {

	val collection = db.collection[BSONCollection]("user")

	val status = Action.async {
		collection.count().map { users =>
			Ok(Json.obj("users" -> users))
		}.recover {
			case e => BadRequest(Json.obj("error" -> e.getLocalizedMessage))
		}
	}

	def register = Action.async { implicit request =>
		implicit val messages = messagesApi.preferred(request)
		NewUser.form.bindFromRequest.fold(
			errors => Future.successful(BadRequest(Json.obj("error" -> errors.errorsAsJson))),
			newUser => collection.insert(newUser).map { result =>
				Ok(Json.obj("status" -> result.message))
			}.recover {
				case e => BadRequest(Json.obj("error" -> e.getLocalizedMessage))
			}
		)
	}

	def verify = Action.async { implicit request =>
		implicit val messages = messagesApi.preferred(request)
		Login.form.bindFromRequest.fold(
			errors => Future.successful(BadRequest(Json.obj("error" -> errors.errorsAsJson))),
			login => collection.find(BSONDocument("user" -> login.username)).one.map {
				case None => BadRequest(Json.obj("error" -> "notFound"))
				case Some(user) => {
					println("user: " + user.getAs[String]("user"))
					println("pass: " + user.getAs[String]("pass").getOrElse(""))
					Ok(Json.obj("verified" ->
							BCrypt.checkpw(login.password, user.getAs[String]("pass").getOrElse(""))))
				}
			}.recover {
				case e => BadRequest(Json.obj("error" -> e.getLocalizedMessage))
			}
		)
	}
}
