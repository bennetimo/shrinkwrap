name := "shrinkwrap"

scalaVersion := "2.12.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature")

libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.0"

enablePlugins(DockerPlugin)

dockerfile in docker := {
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  val dockerDir = target.value / "docker"

  val dockerFile = new Dockerfile {
    from("openjdk:11-slim")
      .maintainer("Tim Bennett")
      .run("apt-get", "update")
      .run("apt-get", "install", "-y", "ffmpeg", "exiftool", "jpegoptim")
      .runRaw("apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*")
      .add(artifact, artifactTargetPath)
      .entryPoint("java", "-jar", artifactTargetPath)
  }

  dockerFile
}

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

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "build"
)