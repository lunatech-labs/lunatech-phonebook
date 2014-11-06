name := "lunatech-phonebook"

version := "1.0-SNAPSHOT"

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

scalaVersion := "2.11.2"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.google.gdata" % "core" % "1.47.1",
  "com.google.api-client" % "google-api-client" % "1.19.0")
