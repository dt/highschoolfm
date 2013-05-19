import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "highschoolfm"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    scalacOptions ++= Seq("-encoding", "UTF-8", "-Xlint","-deprecation", "-unchecked", "-feature")
  )

}
