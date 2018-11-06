import sbt.Keys.scalaVersion
import sbtassembly.AssemblyPlugin.autoImport.assemblyJarName
import ReleaseTransformations._

ThisBuild / scalaVersion := "2.12.7"
ThisBuild / organization := "io.coderunner"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature")

enablePlugins(DockerPlugin)

lazy val publishDocker = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val ref: ProjectRef = extracted.get(thisProjectRef)
  extracted.runAggregated(
    sbtdocker.DockerKeys.dockerBuildAndPush in sbtdocker.DockerPlugin.autoImport.docker in ref,
    st)
  st
})

lazy val shrinkwrap = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "shrinkwrap",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "build",
    scalafmtOnCompile := true,
    libraryDependencies += "com.github.scopt"           %% "scopt"          % "3.7.0",
    libraryDependencies += "ch.qos.logback"             % "logback-classic" % "1.2.3",
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.0",
    libraryDependencies += "org.scalactic"              %% "scalactic"      % "3.0.5",
    libraryDependencies += "org.scalatest"              %% "scalatest"      % "3.0.5" % "test",
    libraryDependencies += "org.scalacheck"             %% "scalacheck"     % "1.14.0" % "test",
    libraryDependencies += "org.mockito"                %% "mockito-scala"  % "1.0.0" % Test,
    assemblyJarName in assembly := s"shrinkwrap-${version.value}.jar",
    dockerfile in docker := {
      val artifact: File     = assembly.value
      val artifactTargetPath = s"/app/${artifact.name}"

      val dockerDir  = target.value / "docker"
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
    ),
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,          
      runClean,                 
      runTest,                  
      setReleaseVersion,        
      commitReleaseVersion,     
      tagRelease,               
      //publishArtifacts,       
      publishDocker,
      setNextVersion,
      commitNextVersion,        
      pushChanges             
    )
  )
