# sbt-jarjar
Repackage a given jar file using [Jar Jar Links]("https://code.google.com/p/jarjar/").

## Example integration with `sbt-assembly`

    jarName in assembly := "my-project.jar",
    jarjar <<= jarjar dependsOn assembly,
    JarJarKeys.jarName in jarjar <<= jarName in assembly,
    JarJarKeys.rules in jarjar := Seq(
      "rule myproblem.** myproblem.nolonger.@1"
    )
