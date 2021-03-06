name := "microservices"

version := "1.0"

scalaVersion := "2.11.7"

lazy val root = (project in file("."))
		.aggregate(`internal-users`)
		.aggregate(`user-profile`)
		.aggregate(`front-end`)
		.aggregate(oauth2)

lazy val `internal-users` = (project in file("internal-users")).enablePlugins(PlayScala)

lazy val `user-profile` = (project in file("user-profile")).enablePlugins(PlayScala)

lazy val `front-end` = (project in file("front-end")).enablePlugins(PlayScala)

lazy val oauth2 = (project in file("oauth2")).enablePlugins(PlayScala)

