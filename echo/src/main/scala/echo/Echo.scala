package echo

@Type
trait Echo {
  @static def echo(msg: String): String

  @static def echoTo(msg: String)(target: String): String
}