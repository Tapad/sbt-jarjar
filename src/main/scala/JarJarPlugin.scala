package sbtjarjar

import java.io.{File, FileInputStream}
import java.nio.file.Files
import java.security.MessageDigest

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

    lazy val digestFile = TaskKey[File]("jarjar-digest-file")
  }

  import JarJarKeys._

  private val JarJarVersion = "1.3"

  private val AsmVersion = "5.1"

  lazy val jarjarSettings: Seq[sbt.Def.Setting[_]] = Seq(
    jarjar := jarjarTask(jarjar).value,
    target in jarjar <<= crossTarget,
    defaultJarName in jarjar := s"${name.value}-${version.value}.jar",
    jarName in jarjar <<= (jarName in jarjar) or (defaultJarName in jarjar),
    outputPath in jarjar := (target in jarjar).value / (jarName in jarjar).value,
    rules in jarjar := Seq.empty[String],
    libraryDependencies ++= Seq(
      "com.googlecode.jarjar" % "jarjar"        % JarJarVersion % "provided",
      "org.ow2.asm"           % "asm"           % AsmVersion    % "provided",
      "org.ow2.asm"           % "asm-commons"   % AsmVersion    % "provided"
    ),
    classpath in jarjar <<= (fullClasspath in Compile) map { classpath =>
      classpath.map(_.data).filter { file =>
        val fileName = file.getName
        (fileName.startsWith("jarjar") && fileName.contains(JarJarVersion)) ||
        (fileName.startsWith("asm") && fileName.contains(AsmVersion))
      }
    },
    jvmOptions in jarjar := Seq("-Xmx2g"),
    digestFile in jarjar := (target in jarjar).value / s"${(jarName in jarjar).value}.sha1"
  )

  private def jarjarTask(key: TaskKey[Unit]): Def.Initialize[Task[Unit]] = Def.task {
    val log = (streams in key).value.log
    val ruleSeq = (rules in key).value
    if (ruleSeq.nonEmpty) {
      val sourceJarFile = (outputPath in key).value
      val targetJarFile = new File(sourceJarFile.getAbsolutePath + "_COPYING_")
      if (sourceJarFile.exists) {
        val digest = createDigestFromFile(sourceJarFile)
        val hasCachedDigest = (digestFile in jarjar).value.exists
        val hasDifferingDigest = !hasCachedDigest || (hasCachedDigest && !(digest sameElements readDigestFromFile((digestFile in jarjar).value)))
        log.debug("Digest of output file" + bytesAsHexString(digest))
        if (hasCachedDigest) {
          log.debug("Digest read from cache: " + bytesAsHexString(readDigestFromFile((digestFile in jarjar).value)))
        } else {
          log.debug("No cached digest exists")
        }
        if (hasDifferingDigest) {
          val rulesFile = createRulesFile(ruleSeq)
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
          log.info(s"Writing digest of repackaged archive to ${(digestFile in jarjar).value}")
          val repackagedDigest = createDigestFromFile(sourceJarFile)
          writeDigestToFile(repackagedDigest, (digestFile in jarjar).value)
        } else {
          log.info(s"`jarjar` up to date: ${sourceJarFile}")
        }
      } else {
        log.error(s"$sourceJarFile does not exist and can not be repackaged.")
      }
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
    IO.writeLines(file, rules)
    file
  }

  private def createDigestFromFile(file: File): Array[Byte] = {
    val msg = MessageDigest.getInstance("SHA-1")
    val fis = new FileInputStream(file)
    var n = 0
    val buffer = new Array[Byte](8192)
    while (n != -1) {
      n = fis.read(buffer)
      if (n > 0) {
        msg.update(buffer, 0, n)
      }
    }
    msg.digest()
  }

  private def writeDigestToFile(digest: Array[Byte], file: File): Unit = {
    val hexDigest = bytesAsHexString(digest)
    IO.write(file, hexDigest)
  }

  private def readDigestFromFile(file: File): Array[Byte] = {
    hexStringAsBytes(IO.read(file))
  }

  private def bytesAsHexString(bytes: Array[Byte]): String = {
    bytes.map("%02x".format(_)).mkString
  }

  private def hexStringAsBytes(string: String): Array[Byte] = {
    string.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).map(Integer.parseInt(_, 16).toByte).toArray
  }
}
