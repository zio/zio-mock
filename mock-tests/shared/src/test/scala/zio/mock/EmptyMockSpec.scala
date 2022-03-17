package zio.mock

import zio._
import zio.mock.Capability.Signature
import zio.mock.internal.MockException
import zio.test.Assertion

import zio.test.{Spec, TestFailure, TestSuccess}

object EmptyMockSpec extends ZIOBaseSpec with MockSpecUtils[Console] {

  import Assertion._
  import MockException._

  def spec: Spec[Any, TestFailure[Any], TestSuccess] = suite("EmptyMockSpec")(
    suite("expect no calls on empty mocks")(
      testValue("should succeed when no call")(
        MockConsole.empty,
        ZIO.when(false)(Console.printLine("foo")).unit,
        isUnit
      ), {

        type M = Signature
        type X = UnexpectedCallException

        testDied("should fail when call happened")(
          MockConsole.empty,
          ZIO.when(true)(Console.printLine("foo")),
          isSubtype[X](
            hasField[X, M]("capability", _.capability, equalTo(MockConsole.PrintLine.signature)) &&
              hasField[X, Any]("args", _.args, equalTo("foo"))
          )
        )
      }
    )
  )
}
