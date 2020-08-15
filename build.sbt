import sbt.Keys.{libraryDependencies, resolvers}


ThisBuild / organization := "ch.japanimpact"
ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "com.pauldijou" %% "jwt-play-json" % "4.2.0",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.64"
)

lazy val api = (project in file("api"))
  .settings(
    version := "2.0-SNAPSHOT",
    libraryDependencies += "com.google.inject" % "guice" % "4.2.2",
    libraryDependencies += cacheApi,
    publishTo := { Some("Japan Impact Repository" at { "https://repository.japan-impact.ch/" + ( if (isSnapshot.value) "snapshots" else "releases" ) } ) },
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
  )


lazy val tools = (project in file("tools"))
  .settings(
    libraryDependencies += "org.bouncycastle" % "bcpkix-jdk15on" % "1.64"
  )


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
      "com.typesafe.play" %% "play-json-joda" % "2.8.1",
      "com.typesafe.play" %% "play-mailer" % "8.0.0",
      "com.typesafe.play" %% "play-mailer-guice" % "8.0.0",
    ),

    resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-language:postfixOps"
    ),

    // Don't add the doc in the zip
    publishArtifact in(Compile, packageDoc) := false

  )
  .aggregate(api, tools)
  .dependsOn(api)


      