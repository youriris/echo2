package echo

class EchoStatic extends Echo.Static {
  def echoStatic(msg: String) = "Hi! " + msg

  def echoStaticTo(msg: String)(target: String) = "Hi, " + target + "! " + msg
}

case class EchoImpl(val greetings: String, override val times: Integer) extends Echo {
  def echo(msg: String) = (1 to times).map(_ => greetings + " " + msg).mkString(" ")

  def echoTo(msg: String)(target: String) = (1 to times).map(_ => greetings + " " + target + "! " + msg).mkString(" ")
}