import com.twitter.scalding._

class DummyJob(args: Args) extends Job(args: Args) {

  val input = args("input")

  val output = args("output")

  Tsv(input).write(Tsv(output))
}
