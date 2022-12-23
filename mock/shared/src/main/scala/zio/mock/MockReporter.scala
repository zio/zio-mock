package zio.mock

import zio.Cause._
import zio._
import zio.mock.internal._
import zio.test._

object MockReporter {
  import Expectation._
  import ExpectationState._
  import InvalidCall._

  def apply[R0, E0, A](
      formatter: ConsoleFormatter = ConsoleFormatter.colorful
  ): TestAspect.PerTest[Nothing, R0, E0, Any] =
    new TestAspect.PerTest[Nothing, R0, E0, Any] {
      import formatter._

      def perTest[R <: R0, E >: E0](test: ZIO[R, TestFailure[E], TestSuccess])(implicit
          trace: Trace
      ): ZIO[R, TestFailure[E], TestSuccess] =
        test
          .catchAll {
            case rt: TestFailure.Runtime[E] =>
              val mockExceptions = extractMockExceptions(rt)

              if (mockExceptions.nonEmpty) handleMockException(mockExceptions: _*)
              else ZIO.fail(rt)
            case other                      => ZIO.fail(other)

          }
          .catchSomeDefect { case me: MockException =>
            handleMockException(me)
          }

      private def extractMockExceptions(rt: TestFailure.Runtime[Any]) = {

        def extract(cause: Cause[Any], exceptions: Chunk[MockException]): Chunk[MockException] =
          cause match {
            case Die(me: MockException, trace)  => exceptions :+ me
            case Then(left, right)              => extract(right, extract(left, exceptions))
            case Fail(me: MockException, trace) => exceptions :+ me
            case Stackless(cause, stackless)    => extract(cause, exceptions)
            case Both(left, right)              => extract(right, extract(left, exceptions))
            case _                              => exceptions
          }

        extract(rt.cause, Chunk.empty)

      }

      private def handleMockException[E](
          e: MockException*
      ): ZIO[Any, TestFailure[E], TestSuccess] = {
        def makeMessages(e: MockException) =
          e match {
            case MockException.UnexpectedCallException(capability, args)     =>
              val mock = capability.mock

              s"${reset("that there should have been no calls to the mock")} ${renderMock(mock)}.  However ${renderCapability(capability, Some(Right(args)))} was called unexpectedly."
            case MockException.UnsatisfiedExpectationsException(expectation) =>
              reset(renderExpectation(expectation))
            case MockException.InvalidCallException(failedMatches)           =>
              reset(renderInvalidCalls(failedMatches))
          }

        ZIO.fail(makeTestFailure(e.map(makeMessages)))
      }

      private def makeTestFailure(messages: Seq[String]) = {

        val ass = Assertion.assertion[Any](
          messages.mkString(" " + prefix(ExpectationState.Unsatisfied))
        )(_ => false)
        TestFailure.Assertion(assert("Your test case")(ass))
      }

      private def prefix(state: ExpectationState) =
        state match {
          case Unsatisfied           => bold(red("           ✖ "))
          case PartiallySatisfied    => bold(yellow("           ⚠ "))
          case Satisfied | Saturated => bold(green("           ✔ "))
        }

      private def renderAnd(and: And[_]): String = {

        val sorted = and.children.sortBy(_.state.value)

        def render(e: Expectation[_]) = {
          val state = e.state

          val invocationCount = e.invocations.fold(0)(_ + _)

          val details = state match {
            case PartiallySatisfied | Unsatisfied =>
              if (invocationCount == 0)
                s"Which was ${bold(red("never"))} called and is considered ${renderState(state)}."
              else if (invocationCount == 1)
                s"Which was only called ${bold(yellow("once"))} and is considered ${renderState(state)}."
              else
                s"Which was only called ${bold(yellow(invocationCount))} times and is considered ${renderState(state)}."
            case Satisfied | Saturated            =>
              s"Which was called ${bold(green(invocationCount))} times as expected and is considered ${renderState(state)}."
          }

          renderExpectation(e, Some(details))
        }

        val expectations = sorted.map(e => prefix(e.state) + render(e)).mkString(s"\n")

        s"""|${bold(blue("some or all"))} of the following expectations:
        |${expectations}""".stripMargin

      }

      private def renderAssertion(ass: Assertion[_]): String = bold(magenta(ass.toString()))

      private def renderMock(in: Mock[_]): String = {
        val fragments = in.getClass.getName.replaceAll("\\$", ".").split("\\.")
        bold(green(fragments.toList.splitAt(fragments.size - 3) match {
          case (namespace, module :: service :: method :: Nil) =>
            s"""$module.$service.$method"""
          case _                                               => fragments.mkString(".")
        }))
      }

      private def renderCall(call: Expectation.Call[_, _, _, _], additionalDetails: Option[String] = None): String = {
        val capability  = call.capability
        val assertion   = call.assertion
        val mock        = call.mock
        val state       = call.state
        val invocations = call.invocations
        val details     = additionalDetails.getOrElse(
          s"However, it was ${bold(red("never"))} called and is considered ${renderState(state)}."
        )

        s"Expected the call ${renderCapability(capability, Some(Left(assertion)))} on ${renderMock(mock)} which satisfies the assertion ${renderAssertion(
          assertion
        )}.  $details"
      }

      private def renderCapability(
          capability: Capability[_, _, _, _],
          ass: Option[Either[Assertion[_], Any]],
          overrideDisplay: String => String = identity
      ): String =
        ass match {
          case Some((Left(ass)))  =>
            bold(
              cyan(s"${capability}(${renderAssertion(ass)}${bold(cyan(")"))}")
            )
          case Some(Right(value)) =>
            bold(
              cyan(s"${capability}(${red(value.toString())}${bold(cyan(")"))}")
            )
          case None               =>
            bold(cyan(s"${overrideDisplay(capability.toString)}"))
        }

      private def renderChain(chain: Expectation.Chain[_]): String = {
        val children = chain.children.map(e => prefix(e.state) + renderExpectation(e)).mkString("\n")
        s"""|the following mocked expectations:
            |${children}
            |""".stripMargin

      }

      private def renderExactly(exactly: Exactly[_]): String = {

        def renderCompleted = exactly.completed match {
          case 0     => s"was ${bold(red("never"))} called"
          case 1     => s"was only called ${bold(yellow("once"))}"
          case count => s"was only called ${bold(yellow(count))} times"
        }

        val details =
          s"It should have been called exactly ${bold(green(exactly.times))} times but $renderCompleted.  As such, it is considered ${renderState(
            exactly.state
          )}."

        renderExpectation(exactly.child, Some(details))
      }

      private def renderExpectation(expectation: Expectation[_], additionalDetails: Option[String] = None): String =
        expectation match {
          case and: And[_]            => renderAnd(and)
          case call: Call[_, _, _, _] => renderCall(call, additionalDetails)
          case chain: Chain[_]        => renderChain(chain)
          case NoCalls(mock)          =>
            s"'NoCalls' on ${renderMock(mock)} should always be handled by the logic that handle the 'UnexpectedCallException'."
          case other: Or[_]           => s"Rendering of Expectation ${other} not supported yet."
          case repeated: Repeated[_]  => renderRepeated(repeated)
          case exactly: Exactly[_]    => renderExactly(exactly)
        }

      private def renderInvalidCall(ic: InvalidCall): String =
        ic match {
          case InvalidArguments(capability, args, assertion)        =>
            s"${renderCapability(capability, Some(Right(args)))} was invoked with the following invalid arguments, '${bold(
              red(args.toString())
            )}', which violated the assertion ${renderAssertion(assertion)}"
          case ic @ InvalidCapability(invoked, expected, assertion) =>
            s"Expected a call to ${renderCapability(expected, Some(Left(assertion)))}, but ${renderCapability(invoked, None, simpleNameRedWithEmphasis)} was invoked instead.  (HINT: Remember order is important when considering mock invocations.)"

          case ipt @ InvalidPolyType(invoked, args, expected, assertion) => ipt.toString()
        }

      private def renderInvalidCalls(invalidCalls: List[InvalidCall]): String = {

        val calls = invalidCalls.map(e => prefix(Unsatisfied) + renderInvalidCall(e)).mkString("\n")

        s"""|expected mock expectations in that it performed the following invalid calls:
            | ${calls}
            |""".stripMargin
      }

      private def renderRepeated(repeated: Repeated[_]): String = {

        def renderCompleted = repeated.completed match {
          case 0     => s"was ${bold(red("never"))} called"
          case 1     => s"was only called ${bold(yellow("once"))}"
          case count => s"was only called ${bold(yellow(count))} times"
        }

        val range = s"${repeated.range.start} to ${repeated.range.end}"

        val details =
          s"It should have been called anywhere from ${bold(green(range))} time but $renderCompleted.  As such, it is considered ${renderState(
            repeated.state
          )}."

        renderExpectation(repeated.child, Some(details))
      }

      private def renderState(state: ExpectationState) = bold(state match {
        case PartiallySatisfied => yellow("partially satisfied")
        case Satisfied          => green("satisfied")
        case Saturated          => green("saturated")
        case Unsatisfied        => red("unsatisfied")
      })

      private def simpleNameRedWithEmphasis(name: String): String =
        name.split('.').reverse.toList match {
          case head :: tail => s"${bold(cyan(tail.reverse.mkString(".") + "."))}${underlined(bold(red(head)))}"
        }
    }

}
