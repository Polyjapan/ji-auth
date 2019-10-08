name := "JIAuth"

enablePlugins(PlayScala, DebianPlugin, JDebPackaging, SystemdPlugin)

version := "1.0"

maintainer in Linux := "Louis Vialar <louis.vialar@japan-impact.ch>"

packageSummary in Linux := "Scala Backend for Japan Impact Auth Platform"
packageDescription := "Scala Backend for Japan Impact Auth Platform"
debianPackageDependencies := Seq("java8-runtime-headless")

lazy val `jiauth` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(ehcache, ws, specs2 % Test, guice)
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "org.mariadb.jdbc" % "mariadb-java-client" % "1.1.7",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.pauldijou" %% "jwt-play" % "0.16.0",
  "net.codingwell" %% "scala-guice" % "4.1.0"
)

libraryDependencies += "com.typesafe.play" %% "play-mailer" % "6.0.1"
libraryDependencies += "com.typesafe.play" %% "play-mailer-guice" % "6.0.1"
libraryDependencies += "ch.japanimpact" %% "jiauthframework" % "0.2-SNAPSHOT"


unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

      