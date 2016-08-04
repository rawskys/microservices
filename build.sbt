name := "microservices"

version := "1.0"

scalaVersion := "2.11.7"

lazy val root = (project in file("."))
	.aggregate(`internal-users`)
	.aggregate(`user-profile`)

lazy val `internal-users` = (project in file("internal-users")).enablePlugins(PlayScala)

lazy val `user-profile` = (project in file("user-profile")).enablePlugins(PlayScala)

