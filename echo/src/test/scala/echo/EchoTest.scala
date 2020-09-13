package echo

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class EchoTest extends AnyFlatSpec {
  "Module" should "delegate call to impl" in {
     Echo.echo("How are you!") shouldBe "Hi! How are you!"

     Echo.echoTo("How are you!")("Tom") shouldBe "Hi, Tom! How are you!"
  }

  "dumpAst()" should "dump AST tree for the type" in {
     Echo.dumpAst should startWith("""ClassDef(Modifiers(ABSTRACT | INTERFACE | DEFAULTPARAM/TRAIT), TypeName("Echo")""")
  }
}