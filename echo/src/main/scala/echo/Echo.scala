package echo

@Type
trait Echo {
  @state def greetings: String

  @state def times: Integer
  
  def echo(msg: String): String 

  def echoTo(msg: String)(target: String): String

  @static def echoStatic(msg: String): String

  @static def echoStaticTo(msg: String)(target: String): String
}