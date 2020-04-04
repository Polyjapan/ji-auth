name := "JIAuthFramework"
organization := "ch.japanimpact"

version := "1.0-SNAPSHOT"

publishArtifact in(Compile, packageDoc) := false
publishArtifact in(Compile, packageSrc) := false
publishArtifact in(Compile, packageBin) := true

scalaVersion := "2.13.1"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.7.4"
libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.7.3"
libraryDependencies += "com.pauldijou" %% "jwt-play-json" % "4.2.0"
libraryDependencies += "org.bouncycastle" % "bcprov-jdk15on" % "1.64"
