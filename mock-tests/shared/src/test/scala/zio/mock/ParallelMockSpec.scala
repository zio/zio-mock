package zio.mock

import zio.ZIO
import zio.mock.module.{ImpureModule, ImpureModuleMock}
import zio.test._

object ParallelMockSpec extends ZIOBaseSpec {

  import Assertion._
  import Expectation._

  def spec: Spec[Any, Any] =
    suite("ParallelMockSpec")(
      test("Count calls for the same expectation") {
        val mock = ImpureModuleMock.SingleParam(equalTo(1), value("r1")).repeats(100 to 100)
        val app  = ZIO.collectAllPar(Vector.fill(100)(ImpureModule.singleParam(1))).provideLayer(mock)
        assertZIO(app)(hasSize[String](equalTo(100)) && hasSameElementsDistinct[String](Seq("r1")))
      },
      test("Collect calls for all expectations") {
        val params = 1 to 100
        val mock   =
          params
            .map(i => ImpureModuleMock.SingleParam(equalTo(i), value(s"r$i")))
            .reduce(_ && _)

        val app = ZIO.collectAllPar(params.map(i => ImpureModule.singleParam(i))).provideLayer(mock)

        val expected = params.map(i => s"r$i")

        assertZIO(app)(hasSameElements(expected))
      }
    )
}
