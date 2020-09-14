package echo

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import scala.collection.mutable.Buffer

@compileTimeOnly("enable macro paradise to expand macro annotations")
class Type2 extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Type2.impl
}

object Type2 {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    
    val inputs = annottees.map(_.tree).toList
    // c.warning(c.enclosingPosition, "tree " + show(c.macroApplication))

    val (interface, module) = inputs match {
      case (interface @ClassDef(mods, typeName, tparams, template)) :: rest =>
        c.warning(c.enclosingPosition, "class mods " + showRaw(interface))
        template match {
          case Template(List(Select(Ident(TermName(scala)), TypeName("AnyRef"))), noSelfType, _) =>
            c.warning(c.enclosingPosition, "class mods x " + showRaw(scala))
        }
        val termName = typeName.toTermName;

        val proxy = (trees: (Buffer[Tree], Buffer[Tree]), m: DefDef) => (mods: Modifiers, name: TermName, tparams: List[TypeDef], vparamss: List[List[ValDef]], tpt: Tree) => {
          val m2 = vparamss.foldLeft(null: Tree)((invoke, params) => {
            val ps = params.map {
              case ValDef(_, n, _, _) => Ident(n)
            }

            invoke match {
              case i: Tree => Apply(i, ps)
              case _ => Apply(Select(Ident(TermName("inst")), name.toTermName), ps)
            }
          })
          // c.warning(c.enclosingPosition, "m2 " + show(m2))

          val defdef = DefDef(Modifiers(), name, tparams, vparamss, tpt, q"""
            val inst = Thread.currentThread().getContextClassLoader.loadClass(${implName(c)(typeName)}).newInstance().asInstanceOf[$typeName]

            $m2
          """)
          if(show(mods).contains("new static()")) {
            trees._1 += m
            trees._2 += defdef 
          } else { 
            trees._1 += defdef 
          }
        }
        
        val (cls, module) = template match {
          case Template(parents, self, body) =>
            val (clsTemplates, objectTemplates) = body.foldLeft((Buffer[Tree](), Buffer[Tree]())) ((trees, t1) => {
              t1 match {
                case m @DefDef(mods, name, tparams, vparamss, tpt, rhs) if mods.hasFlag(Flag.DEFERRED) =>
                  c.warning(c.enclosingPosition, "m2 " + show(mods))
                  proxy(trees, m)(mods, name, tparams, vparamss, tpt)
                case m @DefDef(mods, name, tparams, vparamss, tpt, Ident(TermName(bodyLiteral))) =>
                  proxy(trees, m)(mods, name, tparams, vparamss, tpt)
                case dd @DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
                  c.warning(c.enclosingPosition, "what " + showRaw(rhs))
                  trees._1 += dd 
                case na => 
                  c.warning(c.enclosingPosition, "NA " + showRaw(na))
                  trees._1 += na
              }
              trees
            })
            
            (
            ClassDef(mods, typeName, tparams, Template(List(Select(Ident(TermName("scala")), TypeName("AnyRef"))), noSelfType, clsTemplates.toList)),
            q"""
              object $termName {
                ..$objectTemplates
                
                def dumpAst = ${showRaw(interface)}
              }
            """
            )
          case _ => null
        }
        c.warning(c.enclosingPosition, "cls " + show(cls))
        c.warning(c.enclosingPosition, "module " + show(module))

        (cls, module)
      case List(x) => (x, null)
    }

    val block = q"""
      $interface
      
      $module
    """
      
    c.Expr[Any](block)
  }
  
  private def implName(c: whitebox.Context)(typeName: c.universe.TypeName) = {
    import c.universe._
    
    val freshName = c.fresh(newTypeName("Probe$"))      
    c.typeCheck(q""" {class $freshName; ()} """) match {        
      case Block(List(t), r) => t.symbol.owner.name + "." + TermName(typeName + "Impl")
    }
  }
}

//class StatefulEcho extends scala.AnyRef {   
//  <paramaccessor> val greeting: String = _;   
//  def <init>(greeting: String) = {     super.<init>();     ()   };   
//  def echo(msg: String): String = {     
//    val inst = Thread.currentThread().getContextClassLoader.loadClass("echo.StatefulEchoImpl").newInstance().asInstanceOf[StatefulEcho];     
//    inst.echo(msg)   
//  };   
//  def echoTo(msg: String)(target: String): String = {     val inst = Thread.currentThread().getContextClassLoader.loadClass("echo.StatefulEchoImpl").newInstance().asInstanceOf[StatefulEcho];     inst.echoTo(msg)(target)   } }	StatefulEcho.scala	/echo/src/main/scala/echo	line 3	Scala Problem
//  }
//}
