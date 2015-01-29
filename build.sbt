sbtPlugin := true

name := "sbt-jarjar"

organization := "com.tapad"

version := "1.0.1"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

val tapadNexusSnapshots = "Tapad Nexus Snapshots" at "http://nexus.tapad.com:8080/nexus/content/repositories/snapshots"

val tapadNexusReleases = "Tapad Nexus Releases" at "http://nexus.tapad.com:8080/nexus/content/repositories/releases"

publishTo <<= (version) { version: String =>
  if (version.endsWith("SNAPSHOT") || version.endsWith("TAPAD"))
    Some(tapadNexusSnapshots)
  else
    Some(tapadNexusReleases)
}

publishMavenStyle := true

scalaVersion := "2.10.4"
