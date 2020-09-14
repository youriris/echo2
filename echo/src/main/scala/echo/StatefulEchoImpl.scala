package echo

class StatefulEchoImpl(val greetings: String) extends StatefulEcho {
  def echo(msg: String) = greetings + " " + msg

  def echoTo(msg: String)(target: String) = greetings + " " + target + "! " + msg
}