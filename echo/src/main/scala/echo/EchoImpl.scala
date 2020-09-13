package echo

class EchoImpl extends Echo {
  def echo(msg: String) = "Hi! " + msg

  def echoTo(msg: String)(target: String) = "Hi, " + target + "! " + msg
}