# echo2
This project demonstrates how Scala Macro as a compiler plugin can be used for mutating soure AST tree and generating more compile-time artifacts.
```scala
@Type
trait Echo {
  def echo(msg: String): String
}
```
When you have this Scala trait(Java Interface), the **@Type** Scala Macro generates a Scala Module(Java Singleton) class, that binds an implementation class to the trait. If you provide an impl class such as:
```scala
class EchoImp extends Echo {
  def echo(msg: String) = "Hello! " + msg
}
```
During compilation, the macro will generate a Scala module such as:
```scala
object Echo extends Echo {
  def echo(msg: String) = // invoke EchoImpl's echo()
}
```
Now, invoking **Echo.echo("How are you!")** from a test will return *Hello! How are you!*.

Also, calling **Echo.dumpAst()** will return the whole AST tree of the **Echo** trait. You can use the AST to derive artifacts for further integration, for instance, database schema generation or inter-operation with other programming languages.
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

Just in case you are confused, mutation of AST or generation of a new Scala module happens during compilation. In your IDE, **Echo.echo("How are you")** will compile(and will not with a wrong signature) even though they are defined as abstract in your **Echo** trait. You're calling the **echo()** method in the companion object of **Echo**, generated by the Scala Macro during compile time. Scala conceptually does not have a static method. Instead, companion objects are used in the form of Scala modules.

This project is a pre-cursor to a Compiler Plugin for Kotlin, as Kotlin does not seem to have as good development support from the community.
