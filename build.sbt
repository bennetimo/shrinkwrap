scalaVersion := "2.12.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature")

libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.0"

assemblyJarName in assembly := s"app-assembly.jar"