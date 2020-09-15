package echo

@Type
trait Echo {
  // immutable member value delegation -> EchoImpl.greetings 
  @field def greetings: String

  // immutable member value delegation -> EchoImpl.times
  @field def times: Integer
  
  // instance call delegation -> EchoImpl.echo(msg)
  def echo(msg: String): String 

  // multiple parameter group support -> EchoImpl.echoTo(msg)(target)
  def echoTo(msg: String)(target: String): String

  // static call delegation -> EchoStatic.echoStatic(msg)
  @static def echoStatic(msg: String): String
}