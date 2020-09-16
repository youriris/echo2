package echo

@Type
trait Echo {
  // immutable member value delegation -> EchoImpl.greetings 
  @field @notnull def greetings: String

  // immutable member delegation with default value -> EchoImpl.times
  @field def times: Integer = 1
  
  // instance call delegation -> EchoImpl.echo(msg)
  @notnull def echo(@notnull msg: String): String 

  // multiple parameter group support -> EchoImpl.echoTo(msg)(target)
  @notnull def echoTo(@notnull msg: String)(target: String = "Tom"): String

  // static call delegation -> EchoStatic.echoStatic(msg)
  @static @notnull def echoStatic(@notnull msg: String): String
}