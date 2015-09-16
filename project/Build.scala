import sbt._
import sbt.Keys._
import xerial.sbt.Pack._


object Build extends sbt.Build {

  val upickleVersion = "0.3.4"
  val scalaTestVersion = "2.2.4"
  val gearpumpVersion = "0.6.2-SNAPSHOT"
  val pegdownVersion = "1.4.2"
  val parboiledVersion = "1.1.7"
  val asmVersion = "5.0.3"
  val configVersion = "1.3.0"
  val akkaVersion = "2.3.6"
  val sprayVersion = "1.3.2"

  val commonSettings =
    packAutoSettings ++
    Seq(
        resolvers ++= Seq(
          "sonatype-snapshot" at "https://oss.sonatype.org/content/repositories/snapshots",
          "patriknw at bintray" at "http://dl.bintray.com/patriknw/maven",
          "bintray/non" at "http://dl.bintray.com/non/maven",
          "clockfly" at "http://dl.bintray.com/clockfly/maven"
      )
    ) ++
    Seq(
    scalaVersion := "2.11.7",
    version := "0.1",
    crossPaths := false,
    publishArtifact in Test := true
  )

  lazy val root = Project(
    id = "gearpump-test-framework",
    base = file("."),
    settings = commonSettings ++ publishPackArchives ++
      Seq(
        // custom settings here
      )
  ).aggregate(yarn_test) dependsOn (yarn_test)

  lazy val yarn_test = Project(
    id = "gearpump-linux-test",
    base = file("linux-test"),
    settings = commonSettings ++ packSettings ++ Seq(
      libraryDependencies ++=Seq(
        "org.xerial" % "xerial-core" % "3.3.6",
        "com.lihaoyi" %% "upickle" % upickleVersion,
        "org.scalatest" % "scalatest_2.11" % scalaTestVersion,
        "com.github.intel-hadoop" %% "gearpump-core" % gearpumpVersion,
        "com.github.intel-hadoop" %% "gearpump-streaming" % gearpumpVersion,
        "org.pegdown" % "pegdown" % pegdownVersion,
        "org.parboiled" % "parboiled-core" % parboiledVersion,
        "org.parboiled" % "parboiled-java" % parboiledVersion,
        "org.ow2.asm" % "asm-all" % asmVersion,
        "com.typesafe" % "config" % configVersion,
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "io.spray" %% "spray-routing-shapeless2" % sprayVersion

      )
    )
  )

}