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
    
    class Accumulator(val itf: Buffer[Tree], val module: Buffer[Tree], val fields: Buffer[(TermName, TypeName)], val statics: Buffer[Tree]) {
      def this() {
        this(Buffer[Tree](), Buffer[Tree](), Buffer[(TermName, TypeName)](), Buffer[Tree]())
      }
    }

    val inputs = annottees.map(_.tree).toList

    val block = inputs match {
      case (itf @ClassDef(mods, typeName, tparams, template)) :: rest =>
        val termName = typeName.toTermName;

        val proxy = (trees: Accumulator, m: DefDef) => (mods: Modifiers, name: TermName, tparams: List[TypeDef], vparamss: List[List[ValDef]], tpt: Tree) => {
          if(mods.annotations.exists(_ match {
            case Apply(Select(New(Ident(TypeName("static"))), termNames.CONSTRUCTOR), List()) => true
            case _ => false
          })) {
            trees.statics += m
            
            val assignInst = ValDef(
              Modifiers(), 
              TermName("inst"), 
              TypeTree(), 
              TypeApply(
                Select(
                  Apply(
                    Select(
                      Apply(
                        Select(
                          Select(
                            Apply(Select(Ident(TermName("Thread")), TermName("currentThread")), List()), 
                            TermName("getContextClassLoader")
                          ), 
                          TermName("loadClass")
                        ), 
                        List(Literal(Constant(implName(c)(typeName, true))))
                      ), 
                      TermName("newInstance")
                    ), 
                    List()
                  ), 
                  TermName("asInstanceOf")
                ), 
                List(Select(Ident(termName), TypeName("Static")))
              )
            )

            val invokeMethodOnInst = vparamss.foldLeft(null: Tree)((invoke, params) => {
              val ps = params.map {
                case ValDef(_, n, _, _) => Ident(n)
              }
  
              invoke match {
                case i: Tree => Apply(i, ps)
                case _ => Apply(Select(Ident(TermName("inst")), name.toTermName), ps)
              }
            })

            val defdef = DefDef(Modifiers(), name, tparams, vparamss, tpt, q"""
              $assignInst
  
              $invokeMethodOnInst
            """)
            trees.module += defdef 
          } else {
            trees.itf += m
          }
        }
        
        import scala.reflect.runtime.universe.Flag._
        template match {
          case Template(parents, self, body) =>
            val trees = body.foldLeft(new Accumulator()) ((trees, t1) => {
              t1 match {
                case p @DefDef(
                          Modifiers(DEFERRED, typeNames.EMPTY, List(Apply(Select(New(Ident(TypeName("field"))), termNames.CONSTRUCTOR), List()))), 
                          valName, 
                          List(), 
                          List(), 
                          Ident(valType), 
                          EmptyTree
                        ) =>
                  trees.fields += ((valName, valType.asInstanceOf[TypeName]))
                  trees.itf += p
                case m @DefDef(mods, name, tparams, vparamss, tpt, rhs) if mods.hasFlag(Flag.DEFERRED) =>
                  proxy(trees, m)(mods, name, tparams, vparamss, tpt)
                case m @DefDef(mods, name, tparams, vparamss, tpt, Ident(TermName(bodyLiteral))) =>
                  proxy(trees, m)(mods, name, tparams, vparamss, tpt)
                case dd @DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
                  c.warning(c.enclosingPosition, "what " + showRaw(rhs))
                  trees.itf += dd
                case na => 
                  c.warning(c.enclosingPosition, "NA " + showRaw(na))
                  trees.itf += na
              }
              trees
            })
            
            val paramTypes = trees.fields.map(p => TypeApply(Ident(TermName("classOf")), List(Ident(p._2))))
            val apply = DefDef(
              Modifiers(),
              TermName("apply"), 
              List(), 
              List(trees.fields.map( p => ValDef(Modifiers(Flag.PARAM), p._1, Ident(p._2), EmptyTree)).toList),
              Ident(typeName), 
              q"""
                val cls = Thread.currentThread().getContextClassLoader.loadClass(${implName(c)(typeName, false)})
                val ctor = cls.getDeclaredConstructor(..$paramTypes)
                ctor.setAccessible(true);
                ctor.newInstance(..${trees.fields.map(_._1)}).asInstanceOf[$typeName]
              """
            )

            val itfx = ClassDef(
              mods, typeName, tparams, 
              Template(List(Select(Ident(TermName("scala")), TypeName("AnyRef"))), noSelfType, trees.itf.toList)
            )
            val module = q"""
              object $termName {
                trait Static {
                  ..${trees.statics}
                }

                ..${trees.module}
                
                $apply
            
                def dumpAst = ${showRaw(itf)}
              }
            """
                
            q"""
              $itfx
              
              $module
            """            
          case _ => itf
        }
        // c.warning(c.enclosingPosition, "module " + show(module))

      case List(x) => x
    }

    c.Expr[Any](block)
  }
  
  private def implName(c: whitebox.Context)(typeName: c.universe.TypeName, isStatic: Boolean) = {
    import c.universe._
    
    val freshName = c.fresh(newTypeName("Probe$"))      
    c.typeCheck(q""" {class $freshName; ()} """) match {        
      case Block(List(t), r) => t.symbol.owner.name + "." + TermName(typeName + (if(isStatic) "Static" else "Impl"))
    }
  }
}