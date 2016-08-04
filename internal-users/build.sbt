name := "internal-users"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
	"org.reactivemongo" %% "reactivemongo" % "0.11.14",
	"org.reactivemongo" %% "play2-reactivemongo" % "0.11.7.play24",
	"org.mindrot" % "jbcrypt" % "0.3m",
	ws
)
