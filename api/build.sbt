name := "JIAuthFramework"

publishArtifact in(Compile, packageDoc) := false
publishArtifact in(Compile, packageSrc) := false
publishArtifact in(Compile, packageBin) := true

libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.8.1"
