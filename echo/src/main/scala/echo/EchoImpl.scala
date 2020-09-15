package echo

class EchoStatic extends Echo.Static {
  def echoStatic(msg: String) = "Hi! " + msg

  def echoStaticTo(msg: String)(target: String) = "Hi, " + target + "! " + msg
}

class EchoImpl(val greetings: String, val times: Integer) extends EchoStatic with Echo {
  def echo(msg: String) = (1 to times).map(_ => greetings + " " + msg).mkString(" ")

  def echoTo(msg: String)(target: String) = (1 to times).map(_ => greetings + " " + target + "! " + msg).mkString(" ")
}