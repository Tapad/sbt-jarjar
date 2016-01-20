import com.typesafe.sbt.SbtPgp
import sbtrelease._
import ReleasePlugin._
import ReleaseStateTransformations._

sbtPlugin := true

name := "sbt-jarjar"

organization := "com.tapad"

scalaVersion := "2.10.6"

useGpg := true

publishTo := {
  val nexus = "https://oss.sonatype.org"
  if (isSnapshot.value)
    Some("snapshots" at s"${nexus}/content/repositories/snapshots")
  else
    Some("releases" at s"${nexus}/service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>http://github.com/Tapad/sbt-jarjar</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://opensource.org/licenses/BSD-3-Clause</url>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:Tapad/sbt-jarjar.git</url>
    <connection>scm:git:git@github.com:Tapad/sbt-jarjar.git</connection>
  </scm>
  <developers>
    <developer>
      <id>jeffo@tapad.com</id>
      <name>Jeffrey Olchovy</name>
      <url>http://github.com/jeffreyolchovy</url>
    </developer>
  </developers>
)

releaseSettings

ReleaseKeys.nextVersion := { (version: String) => Version(version).map(_.bumpBugfix.asSnapshot.string).getOrElse(versionFormatError) }

ReleaseKeys.releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)
