import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._
import sbtjarjar._
import JarJarPlugin._

object ScaldingProject extends Build {

  val ScalaVersion = "2.10.6"

  val ScaldingVersion = "0.15.0"

  val ParquetVersion = "1.6.0"

  val AvroVersion = "1.7.6"

  val AlgebirdVersion = "0.10.2"

  val HadoopVersion = "2.3.0-cdh5.0.1"

  val commonSettings = Seq(
    organization := "com.tapad",
    scalaVersion := ScalaVersion,
    scalacOptions := Seq("-deprecation", "-language:_"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.3",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test",
      "org.mockito" % "mockito-core" % "1.9.5" % "test"
    )
  )

  val commonExcludedJars = Set(
    "servlet-api-2.5.jar"
  )

  val commonMergeStrategy: PartialFunction[String, MergeStrategy] = {
    case fp if fp.endsWith("pom.properties") => MergeStrategy.discard
    case fp if fp.endsWith("pom.xml") => MergeStrategy.discard
    case fp if fp.endsWith(".class") => MergeStrategy.last
    case fp if fp.endsWith(".html") => MergeStrategy.discard
  }

  val commonAssemblySettings = assemblySettings ++ Seq(
    excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
      cp.filter(jar => commonExcludedJars.apply(jar.data.getName))
    },
    mergeStrategy in assembly <<= (mergeStrategy in assembly) { dedup =>
      commonMergeStrategy orElse {
        case fp => dedup(fp)
      }
    }
  )

  lazy val root = Project(
    "root",
    file(".")
  ).aggregate(jobs)

  lazy val jobs = Project(
    "jobs",
    file("jobs"),
    settings = commonSettings ++ commonAssemblySettings ++ jarjarSettings ++ Seq(
      libraryDependencies ++= Seq(
        "com.twitter"        %% "scalding-core"         % ScaldingVersion,
        "com.twitter"        %% "scalding-avro"         % ScaldingVersion,
        "com.twitter"        %% "algebird-core"         % AlgebirdVersion,
        "com.twitter"         % "parquet-hadoop"        % ParquetVersion,
        "com.twitter"         % "parquet-avro"          % ParquetVersion,
        "com.twitter"        %% "parquet-scala"         % ParquetVersion,
        "org.apache.avro"     % "avro"                  % AvroVersion,
        "org.apache.avro"     % "avro-mapred"           % AvroVersion,
        "org.apache.hadoop"   % "hadoop-client"         % HadoopVersion
      ),
      jarName in assembly := "herp-derp.jar",
      mainClass in (Compile, run) := Some("com.twitter.scalding.Tool"),
      mainClass in assembly := Some("com.twitter.scalding.Tool"),
      jarjar <<= jarjar dependsOn assembly,
      JarJarKeys.jarName in jarjar <<= jarName in assembly,
      JarJarKeys.rules in jarjar := Seq(
        s"rule parquet.** parquet.jarjar.@1"
      )
    )
  )
}
