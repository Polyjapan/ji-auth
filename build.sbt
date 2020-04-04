import sbt.Keys.{libraryDependencies, resolvers}

lazy val api = (project in file("api"))

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, DebianPlugin, JDebPackaging, SystemdPlugin)
  .settings(
    name := "JIAuth",
    version := "1.0",
    scalaVersion := "2.13.1",

    maintainer in Linux := "Louis Vialar <louis.vialar@japan-impact.ch>",
    packageSummary in Linux := "Scala Backend for Japan Impact Auth Platform",
    packageDescription := "Scala Backend for Japan Impact Auth Platform",
    debianPackageDependencies := Seq("java8-runtime-headless"),

    libraryDependencies ++= Seq(ehcache, ws, specs2 % Test, guice,
      "mysql" % "mysql-connector-java" % "5.1.34",
      "org.mariadb.jdbc" % "mariadb-java-client" % "1.1.7",
      "org.mindrot" % "jbcrypt" % "0.3m",

      evolutions,
      jdbc,
      "org.playframework.anorm" %% "anorm" % "2.6.4",
      "org.bouncycastle" % "bcprov-jdk15on" % "1.64",
      "de.mkammerer" % "argon2-jvm" % "2.6",

      "com.typesafe.play" %% "play-json" % "2.8.1",
      "com.typesafe.play" %% "play-json-joda" % "2.8.1",
      "ch.japanimpact" %% "jiauthframework" % "1.0-SNAPSHOT",
      "com.typesafe.play" %% "play-mailer" % "8.0.0",
      "com.typesafe.play" %% "play-mailer-guice" % "8.0.0",
      "com.pauldijou" %% "jwt-play-json" % "4.2.0"

    ),

    resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation"
    ),

    // Don't add the doc in the zip
    publishArtifact in(Compile, packageDoc) := false

  )
  .dependsOn(api)


      