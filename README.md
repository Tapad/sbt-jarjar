## Example integration with `sbt-assembly`

    jarName in assembly := "my-project.jar",
    jarjar <<= jarjar dependsOn assembly,
    JarJarKeys.jarName in jarjar <<= jarName in assembly,
    JarJarKeys.rules in jarjar := Seq(
      "rule myproblem.** myproblem.nolonger.@1"
    )
