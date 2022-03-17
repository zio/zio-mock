package zio.mock

import zio._
import zio.mock.Capability.Signature
import zio.mock.Expectation._
import zio.test._

object Scala3PoCSpec extends ZIOBaseSpec {
  trait Srv1 {
    val action: UIO[Int]
  }

  class GenericMock[R: Tag: EnvironmentTag](makeCompose: (Proxy, Runtime[Any]) => R) extends Mock[R] {
    override protected[mock] val compose: URLayer[Proxy, R] = {
      for {
        proxy <- ZIO.service[Proxy]
        rts   <- Mock.withRuntime[Any]
      } yield makeCompose(proxy, rts)
    }.toLayer
  }

  def mockSrv[Srv: Tag: EnvironmentTag, E: EnvironmentTag, A: EnvironmentTag](
      call: Srv => ZIO[_, E, A]
  )(methodName: String, compose: (Proxy, Runtime[Any]) => Srv): Capability[Srv, Unit, E, A] =
    new Mock.Effect3(new GenericMock[Srv](compose), methodName)

  def spec = suite("Scala3PoCSpec")(
    test("mock duplication") {
      check(Gen.int, Gen.int) { (i1, i2) =>
        val c1 = mockSrv[Srv1, Nothing, Int](_.action)(
          "action",
          (proxy, _) =>
            new Srv1 {
              override val action: UIO[Int] = proxy.invoke(Signature.simple[Srv1, Unit, Nothing, Int]("action"), ())
            }
        ).apply(value(i1))

        val c2 = mockSrv[Srv1, Nothing, Int](_.action)(
          "action",
          (proxy, _) =>
            new Srv1 {
              override val action: UIO[Int] = proxy.invoke(Signature.simple[Srv1, Unit, Nothing, Int]("action"), ())
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
