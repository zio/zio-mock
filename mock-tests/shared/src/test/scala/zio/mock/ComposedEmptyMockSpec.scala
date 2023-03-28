package zio.mock

import zio.mock.internal.MockException
import zio.test._
import zio.test.MockUtils._
import zio.{Clock, Console, ZIO}

import java.io.IOException

object ComposedEmptyMockSpec extends ZIOBaseSpec {

  import Assertion._
  import Expectation._
  import MockException._
  import testing._

  def branchingProgram(predicate: Boolean): ZIO[Any, IOException, Unit] =
    ZIO
      .succeed(predicate)
      .flatMap {
        case true  => Console.readLine
        case false => Clock.nanoTime
      }
      .unit

  def spec = suite("ComposedEmptyMockSpec")(
    suite("expect no calls on empty mocks")(
      test("should succeed when no calls on Console") {
        branchingProgram(false).as(assertTrue(true))
      } @@ TestAspects.withEnv(
        (MockConsole.empty ++ MockClock.NanoTime(value(42L))).build
      ),
      test("should fail when call on Console happened") {
        type M = Capability[Console, Unit, IOException, String]
        type X = UnexpectedCallException[Console, Unit, IOException, String]

        swapFailure(branchingProgram(true)).map { e =>
          assert(e)(
            isSubtype[X](
              hasField[X, M]("capability", _.capability, equalTo(MockConsole.ReadLine)) &&
                hasField[X, Any]("args", _.args, equalTo(()))
            )
          )
        }
      } @@ TestAspects.withEnv(
        (MockConsole.empty ++ MockClock.empty).build
      ),
      test("should succeed when no calls on Clock") {
        branchingProgram(true).as(assertTrue(true))
      } @@ TestAspects.withEnv(
        (MockClock.empty ++ MockConsole.ReadLine(value("foo"))).build
      ),
      test("should fail when call on Clock happened") {

        type M = Capability[Clock, Unit, Nothing, Long]
        type X = UnexpectedCallException[Clock, Unit, Nothing, Long]
        swapFailure(branchingProgram(false)).map { e =>
          assert(e)(
            isSubtype[X](
              hasField[X, M]("capability", _.capability, equalTo(MockClock.NanoTime)) &&
                hasField[X, Any]("args", _.args, equalTo(()))
            )
          )
        }
      } @@ TestAspects.withEnv(
        (MockClock.empty ++ MockConsole.empty).build
      )
    )
  ) @@ MockReporter()
}
