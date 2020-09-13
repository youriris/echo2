package echo

@Type
trait Echo {
  def echo(msg: String): String

  def echoTo(msg: String)(target: String): String
}