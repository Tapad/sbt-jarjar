import sbtassembly.Plugin._
import AssemblyKeys._
import sbtjarjar._
import JarJarPlugin._

scalaVersion := "2.10.5"

assemblySettings

jarjarSettings

jarName in assembly := "my-project.jar"

mainClass in (Compile, run) := Some("myproject.Boot")

mainClass in assembly := Some("myproject.Boot")

jarjar <<= jarjar dependsOn assembly

JarJarKeys.jarName in jarjar <<= jarName in assembly

JarJarKeys.rules in jarjar := Seq(
  "rule myproblem.** myproblem.nolonger.@1"
)
