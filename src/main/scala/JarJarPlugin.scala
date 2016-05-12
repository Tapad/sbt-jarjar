package sbtjarjar

import java.io.{File, FileWriter}
import java.nio.file.Files

import sbt._
import Keys._

object JarJarPlugin extends Plugin {

  lazy val jarjar = TaskKey[Unit]("jarjar", "Repackage a given jar file using Jar Jar Links.")

  object JarJarKeys {

    lazy val jarName = TaskKey[String]("jarjar-jar-name")

    lazy val defaultJarName = TaskKey[String]("jarjar-default-jar-name")

    lazy val outputPath = TaskKey[File]("jarjar-output-path")

    lazy val rules = TaskKey[Seq[String]]("jarjar-rules")

    lazy val classpath = TaskKey[Seq[File]]("jarjar-classpath")

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
    libraryDependencies ++= Seq(
      "com.googlecode.jarjar" % "jarjar"        % "1.3" % "provided",
      "org.ow2.asm"           % "asm"           % "5.1" % "provided",
      "org.ow2.asm"           % "asm-commons"   % "5.1" % "provided"
    ),
    classpath in jarjar <<= (fullClasspath in Compile) map { classpath =>
      classpath.map(_.data).filter { file =>
        file.getName.contains("jarjar") || file.getName.contains("asm")
      }
    },
    jvmOptions in jarjar := Seq("-Xmx2g")
  )

  private def jarjarTask(key: TaskKey[Unit]): Def.Initialize[Task[Unit]] = Def.task {
    val log = (streams in key).value.log
    val ruleSeq = (rules in key).value
    if (ruleSeq.nonEmpty) {
      val rulesFile = createRulesFile(ruleSeq)
      val sourceJarFile = (outputPath in key).value
      val targetJarFile = new File(sourceJarFile.getAbsolutePath + "_COPYING_")
      log.info(s"Repackaging $sourceJarFile ...")
      val options = ForkOptions(
        bootJars = (classpath in key).value,
        runJVMOptions = (jvmOptions in key).value,
        outputStrategy = Some(LoggedOutput(log))
      )
      val arguments = Seq(
        "com.tonicsystems.jarjar.Main",
        "process",
        rulesFile.getAbsolutePath,
        sourceJarFile.getAbsolutePath,
        targetJarFile.getAbsolutePath
      )
      val result = Fork.java(options, arguments)
      log.debug(s"Process exited with status: $result")
      targetJarFile.renameTo(sourceJarFile)
      rulesFile.delete()
    } else {
      log.warn("No rules defined for `jarjar` task. No repackaging necessary.")
      val sourceJarFile = (outputPath in key).value
      val targetJarFile = new File(sourceJarFile.getAbsolutePath + "_COPYING_")
      Files.copy(sourceJarFile.toPath, targetJarFile.toPath)
      targetJarFile.renameTo(sourceJarFile)
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
