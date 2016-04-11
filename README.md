# sbt-jarjar
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.tapad/sbt-jarjar/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.tapad/sbt-jarjar)

Repackage a given jar file using [Jar Jar Links]("https://code.google.com/p/jarjar/").

Add the following declaration to `project/plugins.sbt`:

```
addSbtPlugin("com.tapad" % "sbt-jarjar" % "1.0.3")
```

## Example integration with `sbt-assembly`

Ensure that your project also includes the `sbt-assembly` plugin.

```
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")
```

```
import sbtassembly.Plugin._
import AssemblyKeys._
import sbtjarjar._
import JarJarPlugin._

val myProject = Project("my-project", file("."), settings = assemblySettings ++ jarjarSettings ++ Seq(
  jarName in assembly := "my-project.jar",
  mainClass in (Compile, run) := Some("myproject.Boot"),
  mainClass in assembly := Some("myproject.Boot"),
  jarjar <<= jarjar dependsOn assembly,
  JarJarKeys.jarName in jarjar <<= jarName in assembly,
  JarJarKeys.rules in jarjar := Seq(
    "rule myproblem.** myproblem.nolonger.@1"
  )
))
```

Projects using `sbt-assembly` version 0.14.0 and greater can receive the same functionality via the built-in shading support. For more information see https://github.com/sbt/sbt-assembly#shading.
