package echo

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class EchoTest extends AnyFlatSpec {
  "Apply constructor" should "have been created" in {
     Echo(greetings = "Yo", times = 2).echo("How are you!") shouldBe "Yo How are you! Yo How are you!"

     Echo(greetings = "Yo", times = 2).echoTo(msg = "How are you!")(target = "Tom") shouldBe "Yo Tom! How are you! Yo Tom! How are you!"
  }

  "Module" should "delegate call to impl" in {
     Echo.echoStatic("How are you!") shouldBe "Hi! How are you!"

     Echo.echoStaticTo("How are you!")("Tom") shouldBe "Hi, Tom! How are you!"
  }

  "dumpAst()" should "dump AST tree for the type" in {
     Echo.dumpAst should startWith("""ClassDef(Modifiers(ABSTRACT | INTERFACE | DEFAULTPARAM/TRAIT), TypeName("Echo")""")
  }
}