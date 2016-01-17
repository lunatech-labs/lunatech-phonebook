name := "lunatech-phonebook"

version := "1.0-SNAPSHOT"

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.lunatech" %% "play-googleopenconnect" % "1.1"
)

resolvers += "Lunatech Artifactory" at "http://artifactory.lunatech.com/artifactory/releases-public"

playScalaSettings
