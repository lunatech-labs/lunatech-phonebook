name := "lunatech-phonebook"

version := "1.0-SNAPSHOT"

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

scalaVersion := "2.11.2"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws)
