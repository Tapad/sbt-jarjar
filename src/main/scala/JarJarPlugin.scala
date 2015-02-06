package sbtjarjar

import java.io.{File, FileWriter}

import sbt._
import Keys._

object JarJarPlugin extends Plugin {

  lazy val jarjar = TaskKey[Unit]("jarjar", "Repackage a given jar file using Jar Jar Links.")

  object JarJarKeys {

    lazy val jarName = TaskKey[String]("jarjar-jar-name")

    lazy val defaultJarName = TaskKey[String]("jarjar-default-jar-name")

    lazy val outputPath = TaskKey[File]("jarjar-output-path")

    lazy val rules = TaskKey[Seq[String]]("jarjar-rules")

    lazy val bin = TaskKey[File]("jarjar-bin")

    lazy val jvmOptions = TaskKey[Seq[String]]("jarjar-jvm-opts")
  }

  import JarJarKeys._

  lazy val jarjarSettings: Seq[sbt.Def.Setting[_]] = Seq(
    jarjar := jarjarTask(jarjar).value,
    target in jarjar <<= crossTarget,
    defaultJarName in jarjar <<= (name, version) map { (name, version) => s"$name-$version.jar" },
    jarName in jarjar <<= (jarName in jarjar) or (defaultJarName in jarjar),
    outputPath in jarjar <<= (target in jarjar, jarName in jarjar) map { (t, s) => t / s },
    rules in jarjar := Seq.empty[String],
    libraryDependencies += "com.googlecode.jarjar" % "jarjar" % "1.3" % "provided",
    bin in jarjar <<= (fullClasspath in Compile) map { classpath =>
      classpath.map(_.data).find(_.getName == "jarjar-1.3.jar").get
    },
    jvmOptions in jarjar := Seq("-Xmx2g")
  )

  private def jarjarTask(key: TaskKey[Unit]): Def.Initialize[Task[Unit]] = Def.task {
    val log = (streams in key).value.log
    val ruleSeq = (rules in key).value
    if (ruleSeq.nonEmpty) {
      val binary = (bin in key).value
      val rulesFile = createRulesFile(ruleSeq)
      val sourceJarFile = (outputPath in key).value
      val targetJarFile = new File(sourceJarFile.getAbsolutePath + "_COPYING_")
      log.info(s"Repackaging $sourceJarFile ...")
      val options = ForkOptions(
        runJVMOptions = (jvmOptions in key).value,
        outputStrategy = Some(LoggedOutput(log))
      )
      val arguments = Seq(binary, "process", rulesFile, sourceJarFile, targetJarFile)
      val process = new Fork("java", Some("-jar")).fork(options, arguments.map(_.toString))
      val result = process.exitValue
      log.debug(s"Process exited with status: $result")
      targetJarFile.renameTo(sourceJarFile)
      rulesFile.delete()
    } else {
      log.error("No rules defined for `jarjar` task. Aborting...")
    }
  }

  private def createRulesFile(rules: Seq[String]): File = {
    val file = File.createTempFile("jarjar-rules_", ".txt")
    val writer = new FileWriter(file)
    rules.map(_ + "\n").foreach(writer.write)
    writer.close()
    file
  }
}
