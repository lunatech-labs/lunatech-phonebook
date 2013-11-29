name := "lunatech-phonebook"

version := "1.0-SNAPSHOT"

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")


libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)     

play.Project.playScalaSettings
