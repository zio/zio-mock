package zio.mock

import zio.mock.Capability.Signature
import zio.mock.internal.MockException
import zio.test.Assertion
import zio.{Clock, Console, ZIO}

import java.io.IOException
import zio.test.{Spec, TestFailure, TestSuccess}

object ComposedEmptyMockSpec extends ZIOBaseSpec with MockSpecUtils[ComposedEmptyMockSpecCompat.Environment] {

  import Assertion._
  import Expectation._
  import MockException._

  def branchingProgram(predicate: Boolean): ZIO[Console with Clock, IOException, Unit] =
    ZIO
      .succeed(predicate)
      .flatMap {
        case true  => Console.readLine
        case false => Clock.nanoTime
      }
      .unit

  def spec: Spec[Any, TestFailure[Any], TestSuccess] = suite("ComposedEmptyMockSpec")(
    suite("expect no calls on empty mocks")(
      testValue("should succeed when no calls on Console")(
        MockConsole.empty ++ MockClock.NanoTime(value(42L)),
        branchingProgram(false),
        isUnit
      ), {
        type M = Signature
        type X = UnexpectedCallException

        testDied("should fail when call on Console happened")(
          MockConsole.empty ++ MockClock.NanoTime(value(42L)),
          branchingProgram(true),
          isSubtype[X](
            hasField[X, M]("capability", _.capability, equalTo(MockConsole.ReadLine.signature)) &&
              hasField[X, Any]("args", _.args, equalTo(()))
          )
        )
      },
      testValue("should succeed when no calls on Clock")(
        MockClock.empty ++ MockConsole.ReadLine(value("foo")),
        branchingProgram(true),
        isUnit
      ), {

        type M = Signature
        type X = UnexpectedCallException

        testDied("should fail when call on Clock happened")(
          MockClock.empty ++ MockConsole.ReadLine(value("foo")),
          branchingProgram(false),
          isSubtype[X](
            hasField[X, M]("capability", _.capability, equalTo(MockClock.NanoTime.signature)) &&
              hasField[X, Any]("args", _.args, equalTo(()))
          )
        )
      }
    )
  )
}

object ComposedEmptyMockSpecCompat {
  type Environment = Console with Clock
}
