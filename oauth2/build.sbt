name := "oauth2"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
	ws,
	"com.nulab-inc" %% "play2-oauth2-provider" % "0.18.0",
	"com.typesafe.play.modules" %% "play-modules-redis" % "2.5.0"
)

resolvers += "sedis-fix" at "https://dl.bintray.com/graingert/maven/"
