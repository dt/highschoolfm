// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += Resolver.file("Local Repository", file("../Play20/repository/local"))(Resolver.ivyStylePatterns)

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-Xlint",
  "-deprecation",
  "-unchecked",
  "-Xfatal-warnings",
  "-Ywarn-value-discard")

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1-SNAPSHOT")