package zio.mock

import zio._
import zio.mock.internal.MockException
import zio.test.Assertion

import java.io.IOException
import zio.test._
import testing._

object EmptyMockSpec extends ZIOBaseSpec {

  import Assertion._
  import MockException._

  def spec = suite("EmptyMockSpec")(
    suite("expect no calls on empty mocks")(
      test("should succeed when no call") {
        ZIO.when(false)(Console.printLine("foo")).as(assertTrue(true))
      },
      test("should fail when call happened") {

        type M = Capability[Console, Any, IOException, Unit]
        type X = UnexpectedCallException[Console, Any, IOException, Unit]

        swapFailure(ZIO.when(true)(Console.printLine("foo"))).map { e =>
          assert(e)(
            isSubtype[X](
              hasField[X, M]("capability", _.capability, equalTo(MockConsole.PrintLine)) &&
                hasField[X, Any]("args", _.args, equalTo("foo"))
            )
          )
        }
      }
    ) @@ TestAspects.withEnv(
      MockConsole.empty.build
    )
  )
}
