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

Just in case you are confused, mutation of AST or generation of a new Scala module happens during compilation. In your IDE, **Echo.echo("How are you")** will compile(and will not with a wrong signature) even though the method is abstract without its body in your **Echo** trait. You're calling the **echo()** method in the companion object of **Echo**, generated by the Scala Macro during compile time. Scala conceptually does not have a static method. Instead, companion objects are used in the form of Scala modules.

This project is a pre-cursor to a Compiler Plugin for Kotlin, as Kotlin does not seem to have as good development support from the community, when it comes to compiler plugin.

Given the full definition of Echo, check what you can do with the following test:
```scala
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

class EchoTest extends AnyFlatSpec {
  "Apply constructor" should "have been created" in {
     val echo1 = Echo(greetings = "Yo", times = 2)
     (echo1.greetings, echo1.times) shouldBe ("Yo", 2)
  }

  "Member method" should "work" in {
     Echo(greetings = "Yo", times = 2).echo("How are you!") shouldBe "Yo How are you! Yo How are you!"

     Echo(greetings = "Yo", times = 1).echoTo(msg = "How are you!")(target = "Tom") shouldBe "Yo Tom! How are you!"
  }

  "Module" should "delegate call to static" in {
     Echo.echoStatic("How are you!") shouldBe "Hi! How are you!"
  }

  "dumpAst()" should "dump AST tree for the type" in {
     Echo.dumpAst should startWith("""ClassDef(Modifiers(ABSTRACT | DEFAULTPARAM/TRAIT), TypeName("Echo")""")
  }
}
```
