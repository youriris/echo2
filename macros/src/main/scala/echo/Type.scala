package echo

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import scala.collection.mutable.Buffer

@compileTimeOnly("enable macro paradise to expand macro annotations")
class Type extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Type.impl
}

object Type {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    
    class Accumulator(val module: Buffer[Tree], val states: Buffer[(TermName, TypeName)]) {
      def this() {
        this(Buffer[Tree](), Buffer[(TermName, TypeName)]())
      }
    }

    val inputs = annottees.map(_.tree).toList

    val (interface, module) = inputs match {
      case (interface @ClassDef(mods, typeName, tparams, template)) :: rest =>
        val termName = typeName.toTermName;

        val proxy = (trees: Accumulator, m: DefDef) => (mods: Modifiers, name: TermName, tparams: List[TypeDef], vparamss: List[List[ValDef]], tpt: Tree) => {
          if(show(mods).contains("new static()")) {
            val m2 = vparamss.foldLeft(null: Tree)((invoke, params) => {
              val ps = params.map {
                case ValDef(_, n, _, _) => Ident(n)
              }
  
              invoke match {
                case i: Tree => Apply(i, ps)
                case _ => Apply(Select(Ident(TermName("inst")), name.toTermName), ps)
              }
            })
  
            val defdef = DefDef(Modifiers(), name, tparams, vparamss, tpt, q"""
              val inst = Thread.currentThread().getContextClassLoader.loadClass(${implName(c)(typeName)}).newInstance().asInstanceOf[$typeName]
  
              $m2
            """)
            trees.module += defdef 
          }
        }
        
        import scala.reflect.runtime.universe.Flag._
        val module = template match {
          case Template(parents, self, body) =>
            val trees = body.foldLeft(new Accumulator()) ((trees, t1) => {
              t1 match {
                case p @DefDef(Modifiers(DEFERRED, typeNames.EMPTY, List(Apply(Select(New(Ident(TypeName("state"))), termNames.CONSTRUCTOR), List()))), valName, List(), List(), Ident(valType), EmptyTree) =>
                  trees.states += ((valName, valType.asInstanceOf[TypeName]))
                case m @DefDef(mods, name, tparams, vparamss, tpt, rhs) if mods.hasFlag(Flag.DEFERRED) =>
                  proxy(trees, m)(mods, name, tparams, vparamss, tpt)
                case m @DefDef(mods, name, tparams, vparamss, tpt, Ident(TermName(bodyLiteral))) =>
                  proxy(trees, m)(mods, name, tparams, vparamss, tpt)
                case dd @DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
                  c.warning(c.enclosingPosition, "what " + showRaw(rhs))
                case na => 
                  c.warning(c.enclosingPosition, "NA " + showRaw(na))
              }
              trees
            })
            
            val applyParams = trees.states.map { p =>
              ValDef(Modifiers(Flag.PARAM), p._1, Ident(p._2), EmptyTree)
            }.toList
            val params = trees.states.map(_._1)
            val apply = DefDef(
              Modifiers(),
              TermName("apply"), 
              List(), 
              List(applyParams),
              Ident(typeName), 
              q"""
                    val cls = Thread.currentThread().getContextClassLoader.loadClass(${implName(c)(typeName)})
                    val ctor = cls.getDeclaredConstructor(classOf[String])
	                  ctor.setAccessible(true);
	                  ctor.newInstance(..$params).asInstanceOf[$typeName]
              """
            )

            q"""
              object $termName {
                ..${trees.module}
            
                $apply
            
                def dumpAst = ${showRaw(interface)}
              }
            """
          case _ => null
        }
        // c.warning(c.enclosingPosition, "module " + show(module))

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