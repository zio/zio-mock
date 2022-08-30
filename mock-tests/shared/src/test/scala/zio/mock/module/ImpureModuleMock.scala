package zio.mock.module

import com.github.ghik.silencer.silent
import zio.mock.{Mock, Proxy}
import zio.{EnvironmentTag, URLayer, Unsafe, ZIO, ZLayer}

/** Example module used for testing ZIO Mock framework.
  */
object ImpureModuleMock extends Mock[ImpureModule] {

  object ZeroParams           extends Method[Unit, Throwable, String]
  object ZeroParamsWithParens extends Method[Unit, Throwable, String]
  object SingleParam          extends Method[Int, Throwable, String]
  object ManyParams           extends Method[(Int, String, Long), Throwable, String]
  object ManyParamLists       extends Method[(Int, String, Long), Throwable, String]
  object Command              extends Method[Unit, Throwable, Unit]
  object ParameterizedCommand extends Method[Int, Throwable, Unit]
  object PolyInput            extends Poly.Method.Input[Throwable, String]
  object PolyError            extends Poly.Method.Error[String, String]
  object PolyOutput           extends Poly.Method.Output[String, Throwable]
  object PolyInputError       extends Poly.Method.InputError[String]
  object PolyInputOutput      extends Poly.Method.InputOutput[Throwable]
  object PolyErrorOutput      extends Poly.Method.ErrorOutput[String]
  object PolyInputErrorOutput extends Poly.Method.InputErrorOutput
  object PolyMixed            extends Poly.Method.Output[Unit, Throwable]
  object PolyBounded          extends Poly.Method.Output[Unit, Throwable]
  object Varargs              extends Method[(Int, Seq[String]), Throwable, String]
  object CurriedVarargs       extends Method[(Int, Seq[String], Long, Seq[Char]), Throwable, String]
  object ByName               extends Effect[Int, Throwable, String]

  object Overloaded {
    object _0 extends Method[Int, Throwable, String]
    object _1 extends Method[Long, Throwable, String]
  }

  object MaxParams extends Method[T22[Int], Throwable, String]

  val compose: URLayer[Proxy, ImpureModule] =
    ZLayer.fromZIO(
      ZIO
        .service[Proxy]
        .flatMap { proxy =>
          withRuntime[Proxy, ImpureModule] { rts =>
            ZIO.succeed {
              new ImpureModule {
                def zeroParams: String = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(ZeroParams)).getOrThrow()
                }

                def zeroParamsWithParens(): String = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(ZeroParamsWithParens)).getOrThrow()
                }

                def singleParam(a: Int): String = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(SingleParam, a)).getOrThrow()
                }

                def manyParams(a: Int, b: String, c: Long): String = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(ManyParams, (a, b, c))).getOrThrow()
                }

                def manyParamLists(a: Int)(b: String)(c: Long): String =
                  Unsafe.unsafe { implicit u =>
                    rts.unsafe.run(proxy(ManyParamLists, a, b, c)).getOrThrow()
                  }

                @silent("side-effecting nullary methods")
                def command: Unit = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(Command)).getOrThrow()
                }

                def parameterizedCommand(a: Int): Unit = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(ParameterizedCommand, a)).getOrThrow()
                }

                def overloaded(n: Int): String = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(Overloaded._0, n)).getOrThrow()
                }

                def overloaded(n: Long): String = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(Overloaded._1, n)).getOrThrow()
                }

                def polyInput[I: EnvironmentTag](v: I): String = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(PolyInput.of[I], v)).getOrThrow()
                }

                def polyError[E <: Throwable: EnvironmentTag](v: String): String =
                  Unsafe.unsafe { implicit u =>
                    rts.unsafe.run(proxy(PolyError.of[E], v)).getOrThrow()
                  }

                def polyOutput[A: EnvironmentTag](v: String): A = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(PolyOutput.of[A], v)).getOrThrow()
                }

                def polyInputError[I: EnvironmentTag, E <: Throwable: EnvironmentTag](v: I): String =
                  Unsafe.unsafe { implicit u =>
                    rts.unsafe.run(proxy(PolyInputError.of[I, E], v)).getOrThrow()
                  }

                def polyInputOutput[I: EnvironmentTag, A: EnvironmentTag](v: I): A =
                  Unsafe.unsafe { implicit u =>
                    rts.unsafe.run(proxy(PolyInputOutput.of[I, A], v)).getOrThrow()
                  }

                def polyErrorOutput[E <: Throwable: EnvironmentTag, A: EnvironmentTag](v: String): A =
                  Unsafe.unsafe { implicit u =>
                    rts.unsafe.run(proxy(PolyErrorOutput.of[E, A], v)).getOrThrow()
                  }

                def polyInputErrorOutput[I: EnvironmentTag, E <: Throwable: EnvironmentTag, A: EnvironmentTag](
                    v: I
                ): A =
                  Unsafe.unsafe { implicit u =>
                    rts.unsafe.run(proxy(PolyInputErrorOutput.of[I, E, A], v)).getOrThrow()
                  }

                def polyMixed[A: EnvironmentTag]: (A, String) = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(PolyMixed.of[(A, String)])).getOrThrow()
                }

                def polyBounded[A <: AnyVal: EnvironmentTag]: A = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(PolyBounded.of[A])).getOrThrow()
                }

                def varargs(a: Int, b: String*): String = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(Varargs, (a, b))).getOrThrow()
                }

                def curriedVarargs(a: Int, b: String*)(c: Long, d: Char*): String =
                  Unsafe.unsafe { implicit u =>
                    rts.unsafe.run(proxy(CurriedVarargs, (a, b, c, d))).getOrThrow()
                  }

                def byName(a: => Int): String = Unsafe.unsafe { implicit u =>
                  rts.unsafe.run(proxy(ByName, a)).getOrThrow()
                }

                def maxParams(
                    a: Int,
                    b: Int,
                    c: Int,
                    d: Int,
                    e: Int,
                    f: Int,
                    g: Int,
                    h: Int,
                    i: Int,
                    j: Int,
                    k: Int,
                    l: Int,
                    m: Int,
                    n: Int,
                    o: Int,
                    p: Int,
                    q: Int,
                    r: Int,
                    s: Int,
                    t: Int,
                    u: Int,
                    v: Int
                ): String = Unsafe.unsafe { implicit unsafe =>
                  rts.unsafe
                    .run(proxy(MaxParams, (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v)))
                    .getOrThrow()
                }
              }
            }
          }
        }
    )

}
