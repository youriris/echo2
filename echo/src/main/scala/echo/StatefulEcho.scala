package echo

//@Type
trait StatefulEcho {
  @state def greetings: String
  
  def echo(msg: String): String 

  def echoTo(msg: String)(target: String): String
}