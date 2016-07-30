package com.rawskys.microservices.userprofile.controllers

import javax.inject.Inject

import play.api.i18n.MessagesApi
import play.api.mvc.Controller
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection

class UserProfileController @Inject()(val reactiveMongoApi: ReactiveMongoApi, val messagesApi: MessagesApi)
		extends Controller with MongoController with ReactiveMongoComponents {

	val collection = db.collection[BSONCollection]("userprofile")


}
