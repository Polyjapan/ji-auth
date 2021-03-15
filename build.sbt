import sbt.Keys.{libraryDependencies, resolvers}


ThisBuild / organization := "ch.japanimpact"
ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "com.pauldijou" %% "jwt-play-json" % "5.0.0",
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
  .enablePlugins(PlayScala, JDebPackaging, SystemdPlugin, JavaServerAppPackaging, DockerPlugin)
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

      // Webauthn imports Jackson 2.11 which is "too recent" for the Play Framework pipeline (Jackson < 2.11)
      // We need to prevent this by making the dependencies intransitive
      "com.yubico" % "webauthn-server-core" % "1.7.0"
        exclude ("com.fasterxml.jackson.core", "jackson-databind")
        exclude ("com.yubico", "yubico-util"),
      "com.yubico" % "yubico-util" % "1.7.0"
        exclude ("com.fasterxml.jackson.core", "jackson-databind")
        exclude ("com.fasterxml.jackson.datatype", "jackson-datatype-jdk8")
        exclude ("com.fasterxml.jackson.dataformat", "jackson-dataformat-cbor"),

      "com.warrenstrange" % "googleauth" % "1.4.0"
    ),

    resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-language:postfixOps"
    ),

    maintainer in Linux := "Louis Vialar <louis@japan-impact.ch>",

    javaOptions in Universal ++= Seq(
      // Provide the PID file
      s"-Dpidfile.path=/dev/null",
      // s"-Dpidfile.path=/run/${packageName.value}/play.pid",

      // Set the configuration to the production file
      s"-Dconfig.file=/usr/share/${packageName.value}/conf/production.conf",

      // Apply DB evolutions automatically
      "-DapplyEvolutions.default=true"
    ),

    dockerExposedPorts in Docker := Seq(80),



    // Don't add the doc in the zip
    publishArtifact in(Compile, packageDoc) := false

  )
  .aggregate(api, tools)
  .dependsOn(api)


      