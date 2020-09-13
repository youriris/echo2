package echo

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

@compileTimeOnly("enable macro paradise to expand macro annotations")
class Type extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Type.impl
}

object Type {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    
    val inputs = annottees.map(_.tree).toList
    // c.warning(c.enclosingPosition, "tree " + show(c.macroApplication))

    val (interface, module) = inputs match {
      case (interface @ClassDef(mods, typeName, tparams, template)) :: rest =>
        val termName = typeName.toTermName;
        
        val module = template match {
          case Template(parents, self, body) =>
            val newTemplate = body.map {
              case m @DefDef(mods, name, tparams, vparamss, tpt, rhs) if mods.hasFlag(Flag.DEFERRED) =>
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

                DefDef(Modifiers(), name, tparams, vparamss, tpt, q"""
                  val inst = Thread.currentThread().getContextClassLoader.loadClass(${implName(c)(typeName)}).newInstance().asInstanceOf[$typeName]

                  $m2
                """)
              case _ => null
            }.filter(_ != null)
            
            q"""
              object $termName {
                ..$newTemplate
                
                def dumpAst = ${showRaw(interface)}
              }
            """
          case _ => null
        }

        (interface, module)
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