import sbt.Keys.scalaVersion
import sbtassembly.AssemblyPlugin.autoImport.assemblyJarName

ThisBuild / scalaVersion := "2.12.7"
ThisBuild / organization := "io.coderunner"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature")

enablePlugins(DockerPlugin)

lazy val shrinkwrap = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    name := "shrinkwrap",
    version := "0.1.0",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "build",
    scalafmtOnCompile := true,

    libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.0",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
    
    assemblyJarName in assembly := s"shrinkwrap-${version.value}.jar",
    dockerfile in docker := {
      val artifact: File = assembly.value
      val artifactTargetPath = s"/app/${artifact.name}"

      val dockerDir = target.value / "docker"
      val projectDir = project.base.getAbsolutePath

      val dockerFile = new Dockerfile {
        from(" jrottenberg/ffmpeg:4.0-alpine")
          .maintainer("Tim Bennett")
          .runRaw("apk --update --no-cache add openjdk8-jre exiftool bash")
          .add(artifact, artifactTargetPath)
          .entryPoint("java", "-jar", artifactTargetPath)
      }

      dockerFile
    },
    imageNames in docker := Seq(
      // Sets the latest tag
      ImageName(
        namespace = Some("bennetimo"),
        repository = name.value,
        tag = Some("latest")
      ),

      // Sets a name with a tag that contains the project version
      ImageName(
        namespace = Some("bennetimo"),
        repository = name.value,
        tag = Some(version.value)
      )
    )
)