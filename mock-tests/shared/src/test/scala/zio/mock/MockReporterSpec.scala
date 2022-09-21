package zio.mock

import zio.test._
import zio._
import testing._
import java.time.Instant
import zio.mock.module._

object MockReporterSpec extends ZIOSpecDefault {
  import Expectation._
  import Assertion._
  import TestAspect.ignore

  val andSuite = suite("And")(
    test("render `And`") {
      ZIO.when(true)(Console.printLine("foo")).as(assertTrue(true))
    } @@ TestAspects.withExpectationsAsEnv(
      MockConsole.PrintLine(equalTo("foo"), unit) && MockConsole.Print(equalTo("bar"), unit)
    )
  )

  val behaviorSuite = suite("Behavior")(
    test("should render `And` correctly.") {
      verifyRendering(andSuite) { summary =>
        val output = summary.failureDetails
        assertTrue(
          summary.fail == 1,
          summary.total == 1,
          output.contains("Your test case"),
          output.contains(
            "some or all of the following expectations:"
          ),
          output.contains(
            "✖ Expected the call zio.mock.MockConsole.Print(equalTo(bar)) on zio.mock.MockConsole which satisfies the assertion equalTo(bar).  Which was never called and is considered unsatisfied"
          ),
          output.contains(
            "✔ Expected the call zio.mock.MockConsole.PrintLine(equalTo(foo)) on zio.mock.MockConsole which satisfies the assertion equalTo(foo).  Which was called 1 times as expected and is considered saturated"
          )
        )
      }
    },
    test("should render `InvalidCall`s correctly.") {
      verifyRendering(invalidCallSuite) { summary =>
        val output = summary.failureDetails
        assertTrue(
          summary.fail == 1,
          summary.total == 1,
          output.contains(
            "Your test case"
          ),
          output.contains(
            "expected mock expectations in that it performed the following invalid calls"
          ),
          output.contains(
            "✖ zio.mock.MockConsole.Print(baz) was invoked with the following invalid arguments, 'baz', which violated the assertion equalTo(foo)"
          )
        )
      }
    },
    test("should render `InvalidCall.InvalidCapability` correctly.") {
      verifyRendering(invalidCapabilitySuite) { summary =>
        val output = summary.failureDetails
        assertTrue(
          summary.fail == 1,
          summary.total == 1,
          output.contains(
            "Your test case"
          ),
          output.contains(
            "expected mock expectations in that it performed the following invalid calls:"
          ),
          output.contains(
            "✖ Expected a call to zio.mock.MockConsole.Print(equalTo(foo)), but zio.mock.MockConsole.PrintLine was invoked instead.  (HINT: Remember order is important when considering mock invocations.)"
          )
        )
      }
    },
    test("should render polymorphic mock failures correctly.") {
      verifyRendering(polySuite) { summary =>
        val output = summary.failureDetails
        assertTrue(
          summary.fail == 1,
          summary.total == 1,
          output.contains(
            "Your test case"
          ),
          output.contains(
            "zio.mock.module.PureModuleMock.PolyInput(baz) was invoked with the following invalid arguments, 'baz', which violated the assertion equalTo(foo)"
          ),
          output.contains(
            "zio.mock.module.PureModuleMock.PolyInput(baz) was invoked with the following invalid arguments, 'baz', which violated the assertion equalTo(foo)"
          )
        )
      }
    },
    test("should render `UnexpectedCallException` correctly.") {
      verifyRendering(unexpectedCallSuite) { summary =>
        val output = summary.failureDetails
        assertTrue(
          summary.fail == 1,
          summary.total == 1,
          output.contains(
            "Your test case"
          ),
          output.contains(
            "that there should have been no calls to the mock zio.mock.MockConsole.  However zio.mock.MockConsole.Print(foo) was called unexpectedly.()"
          )
        )
      }
    },
    test("should render `UnexpectedSatisfiedExpectationException` correctly.") {
      verifyRendering(unsatisfiedExpectationSuite) { summary =>
        val output = summary.failureDetails
        assertTrue(
          summary.fail == 1,
          summary.total == 1,
          output.contains(
            "Your test case"
          ),
          output.contains(
            "the following mocked expectations:"
          ),
          output.contains(
            "⚠ Expected the call zio.mock.MockConsole.Print(equalTo(foo)) on zio.mock.MockConsole which satisfies the assertion equalTo(foo).  It should have been called exactly 2 times but was only called once.  As such, it is considered partially satisfied."
          ),
          output.contains(
            "✖ Expected the call zio.mock.MockClock.Instant(isUnit()) on zio.mock.MockClock which satisfies the assertion isUnit().  However, it was never called and is considered unsatisfied."
          ),
          output.contains(
            "✖ Expected the call zio.mock.MockConsole.PrintLine(equalTo(bar)) on zio.mock.MockConsole which satisfies the assertion equalTo(bar).  It should have been called anywhere from 2 to 5 time but was never called.  As such, it is considered unsatisfied."
          )
        )
      }
    },
    test("should not effect non-mock test failures.") {
      val testSuite = test("I fail") {
        assertTrue(false)
      } @@ MockReporter()

      executeSpec(testSuite).map { summary =>
        assertTrue(
          summary.fail == 1,
          summary.total == 1
        )
      }

    }
  ) @@ ignore // TODO: reenable this test suite
  val expectation   =
    MockConsole.Print(Assertion.equalTo("foo"), unit).twice ++ MockClock.Instant(
      value(Instant.ofEpochMilli(0))
    ) ++ MockConsole.PrintLine(Assertion.equalTo("bar"), unit).repeats(2 to 5)

  val invalidCallSuite = suite("InvalidCall")(
    test("render invalid call") {
      ZIO.when(true)(Console.print("baz")).as(assertTrue(true))
    } @@ TestAspects.withEnv(expectation.build)
  )

  val invalidCapabilitySuite = suite("InvalidCapabilityException")(
    test("render invalid capability") {
      ZIO
        .when(true)(Console.print("foo") *> Console.printLine("bar") *> Console.printLine("bar"))
        .as(assertTrue(true))
    } @@ TestAspects.withEnv(expectation.build)
  )

  val unexpectedCallSuite = suite("UnexpectedCallException")(
    test("render unexpected") {
      ZIO.when(true)(Console.print("foo")).as(assertTrue(true))
    } @@ TestAspects.withEnv(MockConsole.empty.build)
  )

  val unsatisfiedExpectationSuite = suite("UnsatisfiedExpectationsException")(
    test("render unsatisfied") {
      ZIO.when(true)(Console.print("foo")).as(assertTrue(true))
    } @@ TestAspects.withEnv(expectation.build)
  )

  val polySuite = suite("Polymorphic")(
    test("render polymorphic") {
      val expectation = PureModuleMock.PolyInput.of[String](equalTo("foo"), value("bar"))

      PureModule.polyInput("baz").as(assertTrue(true)).provideLayer(expectation.toLayer)
    } @@ TestAspects.withEnv(ZIO.succeed(ZEnvironment.empty))
  )

  val visualCheckSuite =
    test("Visualize") {
      val testCase = suite("Show failures with formatting")(
        andSuite,
        invalidCallSuite,
        invalidCapabilitySuite,
        polySuite,
        unexpectedCallSuite,
        unsatisfiedExpectationSuite
      ) @@ MockReporter()

      executeSpec(testCase, true).as(assertTrue(true))
    }

  def verifyRendering(
      spec: Spec[Scope, Any]
  )(ass: (Summary) => TestResult) =
    for {
      summary <- executeSpec(spec @@ MockReporter(ConsoleFormatter.bland))
    } yield ass(summary)

  override def spec = suite("MockReporterSpec")(
    behaviorSuite,
    visualCheckSuite @@ ignore // Remove `ignore` to view failures in all their colorful glory.
  )

}
