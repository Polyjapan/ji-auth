name := "JIAuthFramework"

publishArtifact in(Compile, packageDoc) := true
publishArtifact in(Compile, packageSrc) := true
publishArtifact in(Compile, packageBin) := true

libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.8.1"
