import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "highschoolfm"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "se.radley" %% "play-plugins-salat" % "1.0.7"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      routesImport += "se.radley.plugin.salat.Binders._",
      templatesImport += "org.bson.types.ObjectId",
      scalacOptions ++= Seq(
        "-encoding",
        "UTF-8",
        "-Xlint",
        "-deprecation",
        "-unchecked",
        "-Xfatal-warnings",
        "-Ywarn-value-discard",
        "-Ywarn-all"),
      initialCommands := """
        import play.core.StaticApplication
        new StaticApplication(new java.io.File("."))
        import models._
        import libs._
        import play.api.libs.json._
        import play.api.Logger
        val pid = management.ManagementFactory.getRuntimeMXBean().getName().split("@")(0)
        val pidfile = new java.io.File("PID")
        val pidfilewriter = new java.io.FileWriter(pidfile)
        pidfilewriter.write(pid.toString)
        pidfilewriter.close()
        pidfile.deleteOnExit()
      """
    )

}
