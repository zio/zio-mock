package zio.mock

import zio._
import zio.mock.Expectation._
import zio.test._

object Scala3PoCSpec extends ZIOBaseSpec {
  trait Srv1 {
    val action: UIO[Int]
  }

  object Srv1Mock1 extends Mock[Srv1] {
    object Action extends Mock.Effect3[Srv1, Unit, Nothing, Int](Srv1Mock1, "action")

    val compose: URLayer[Proxy, Srv1] = {
      for {
        proxy <- ZIO.service[Proxy]
        _     <- withRuntime[Any]
      } yield new Srv1 {
        override val action: UIO[Int] = proxy(Action)
      }
    }.toLayer
  }

  object Srv1Mock2 extends Mock[Srv1] {
    object Action extends Mock.Effect3[Srv1, Unit, Nothing, Int](Srv1Mock2, "action")

    val compose: URLayer[Proxy, Srv1] = {
      for {
        proxy <- ZIO.service[Proxy]
        _     <- withRuntime[Any]
      } yield new Srv1 {
        override val action: UIO[Int] = proxy(Action)
      }
    }.toLayer
  }

  class GenericMock[R: Tag: EnvironmentTag](makeCompose: (Proxy, Runtime[Any], () => Mock[R]) => R) extends Mock[R] {
    override protected[mock] val compose: URLayer[Proxy, R] = {
      for {
        proxy <- ZIO.service[Proxy]
        rts   <- Mock.withRuntime[Any]
      } yield makeCompose(proxy, rts, () => this)
    }.toLayer
  }

  def mockSrv[Srv: Tag: EnvironmentTag, E: EnvironmentTag, A: EnvironmentTag](
      call: Srv => ZIO[_, E, A]
  )(methodName: String, compose: (Proxy, Runtime[Any], () => Mock[Srv]) => Srv): Capability[Srv, Unit, E, A] =
    new Mock.Effect3(new GenericMock[Srv](compose), methodName)

  def spec = suite("Scala3PoCSpec")(
    test("mock duplication") {
      check(Gen.int, Gen.int) { (i1, i2) =>
        val c1 = mockSrv[Srv1, Nothing, Int](_.action)(
          "action",
          (proxy, _, mock) =>
            new Srv1 {
              override val action: UIO[Int] = proxy(new Mock.Effect3[Srv1, Unit, Nothing, Int](mock(), "action"))
            }
        ).apply(value(i1))

        val c2 = mockSrv[Srv1, Nothing, Int](_.action)(
          "action",
          (proxy, _, mock) =>
            new Srv1 {
              override val action: UIO[Int] = proxy(new Mock.Effect3[Srv1, Unit, Nothing, Int](mock(), "action"))
            }
        ).apply(value(i2))

        val mockEnv = c1 && c2

        val action = for {
          srv  <- ZIO.service[Srv1]
          pair <- srv.action.zipPar(srv.action)
        } yield assertTrue(pair == (i1, i2) || pair == (i2, i1))

        action.provideLayer(mockEnv)
      }
    }
  )
}
