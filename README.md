# echo2
This project shows how Scala Macro as a compiler plugin can be used to generate compile-time artifacts.
```
@Type
trait Echo {
  def echo(msg: String): String
}
```
When you have this Scala trait(Java Interface), the @Type Scala Macro generates a Scala Module(Java Singleton) class, that binds an implementation class to the trait. If you provide an impl class such as:
```
class EchoImp extends Echo {
  def echo(msg: String) = "Hello! " + msg
}
```
During compilation, the macro will generate a Scala module such as:
```
object Echo extends Echo {
  def echo(msg: String) = <invoke-EchoImpl's-echo()>
}
```
So, invoking Echo.echo("How are you!") from a test will return "Hello! How are you!".

Also, calling Echo.toAST() will return the whole AST tree of the Echo trait. You can use the AST to derive artifacts for further integration for instance database schema generation or interoperation with other programming languages.
```
ClassDef(
  Modifiers(ABSTRACT | INTERFACE | DEFAULTPARAM/TRAIT), 
  TypeName("Echo"), 
  List(), 
  Template(
    List(Select(Ident(scala), TypeName("AnyRef"))), 
    noSelfType, 
    List(
      DefDef(
        Modifiers(DEFERRED), 
        TermName("echo"), 
        List(), 
        List(List(ValDef(Modifiers(PARAM), TermName("msg"), Ident(TypeName("String")), EmptyTree))), 
        Ident(TypeName("String")), 
        EmptyTree
      ), 
      DefDef(
        Modifiers(DEFERRED), 
        TermName("echoTo"), 
        List(), 
        List(List(ValDef(Modifiers(PARAM), TermName("msg"), Ident(TypeName("String")), EmptyTree)), List(ValDef(Modifiers(PARAM), TermName("target"), Ident(TypeName("String")), EmptyTree))), 
        Ident(TypeName("String")), 
        EmptyTree
      )
    )
  )
)
```
