import sbtrelease._
import ReleasePlugin._
import ReleaseStateTransformations._

sbtPlugin := true

name := "sbt-jarjar"

organization := "com.tapad"

scalaVersion := "2.10.6"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

val TapadNexusSnapshots = "Tapad Nexus Snapshots" at "http://nexus.tapad.com:8080/nexus/content/repositories/snapshots"

val TapadNexusReleases = "Tapad Nexus Releases" at "http://nexus.tapad.com:8080/nexus/content/repositories/releases"

publishTo := { if (version.value.endsWith("SNAPSHOT")) Some(TapadNexusSnapshots) else Some(TapadNexusReleases) }

publishMavenStyle := true

releaseSettings

ReleaseKeys.nextVersion := { (version: String) => Version(version).map(_.bumpBugfix.asSnapshot.string).getOrElse(versionFormatError) }

ReleaseKeys.releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion
)
