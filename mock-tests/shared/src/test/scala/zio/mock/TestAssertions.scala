package zio.mock

import zio.Trace
import zio.mock.internal.InvalidCall
import zio.test.{ErrorMessage => M, _}
import InvalidCall._
import zio.mock.internal.MockException._

object TestAssertions {

  type AnyInvalidArguments  = InvalidArguments[_, _, _, _]
  type AnyInvalidCapability = InvalidCapability[_, _, _, _, _, _, _, _]
  type AnyInvalidPolyType   = InvalidPolyType[_, _, _, _, _, _, _, _]

  def kindaEqualTo[A](expected: A)(implicit trace: Trace) = {

    def compareCalls(actual: InvalidCall, expected: InvalidCall, additionalInfo: String = ""): TestTrace[Boolean] = {
      val msg = M.pretty(actual) + M.equals + M.pretty(expected) + s"${additionalInfo}(ignoring Assertions)"
      (actual, expected) match {
        case (invA: AnyInvalidArguments, invE: AnyInvalidArguments)   =>
          TestTrace.boolean(invA.args == invE.args && invA.invoked == invE.invoked)(msg)
        case (invA: AnyInvalidCapability, invE: AnyInvalidCapability) =>
          TestTrace.boolean(invA.expected == invE.expected && invA.invoked == invE.invoked)(msg)
        case (invA: AnyInvalidPolyType, invE: AnyInvalidPolyType)     =>
          TestTrace
            .boolean(invA.args == invE.args && invA.invoked == invE.invoked && invA.expected == invE.expected)(msg)
        case (invA, invE)                                             =>
          TestTrace.boolean(false)(
            M.text("kindaEqualTo does not support comparing") + M.pretty(invA) + M.text("and") + M.pretty(invE)
          )
      }
    }

    Assertion[A](
      TestArrow
        .make[A, Boolean] { actual =>
          (actual, expected) match {
            case (actual: InvalidCall, expected: InvalidCall)                 => compareCalls(actual, expected)
            case (InvalidCallException(callsA), InvalidCallException(callsB)) =>
              TestTrace.boolean(callsA.size == callsB.size)(
                M.text(s"Expected ${callsB.size} InvalidCall(s), received ${callsA.size} instead.")
              ) && callsA
                .zip(callsB)
                .zipWithIndex
                .map { case ((callA, callB), i) => compareCalls(callA, callB, s" at index ${i} ") }
                .reduce(_ && _)
            case _                                                            => TestTrace.boolean(false)(M.pretty("Is not an InvalidCall nor a InvalidCallException"))
          }
        }
        .withCode(s"kindaEqualTo")
    )
  }
}
