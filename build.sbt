import sbt.Keys.{libraryDependencies, scalacOptions}

name := "echo2"

lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.12.3",
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
)

lazy val macros = (project in file("macros")).settings(
  commonSettings,
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
)

lazy val echo = (project in file("echo")).settings(
  commonSettings,
  libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.0",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0" % "test"
) dependsOn macros

version := "0.1"
