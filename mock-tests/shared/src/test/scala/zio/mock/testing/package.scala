package zio.mock

import zio._
import zio.test._

package object testing {

  private def defaultAbsorber[E]: E => Throwable = {
    case e: Throwable => e
    case e            => CapturingException(e)
  }

  def swapFailure[R, E, A](
      zio: ZIO[R, E, A],
      absorber: E => Throwable = defaultAbsorber[E]
  ): ZIO[R, A, Throwable] =
    zio
      .absorbWith(absorber)
      .flip
      .catchAll(UnexpectedSuccess.makeZIO)

  object SilentTestOutput extends TestOutput {
    def print(executionEvent: ExecutionEvent): ZIO[Any, Nothing, Unit] = ZIO.unit
  }

  def executeSpec[E](spec: ZSpec[Any with Scope, E], showSpecOutput: Boolean = false) = {
    val testOutput = if (showSpecOutput) TestOutput.live else ZLayer.succeed(SilentTestOutput)
    TestExecutor
      .default(
        testEnvironment ++ Scope.default,
        (Console.live >>> TestLogger.fromConsole(
          Console.ConsoleLive
        ) >>> ExecutionEventPrinter.live >>> testOutput >>> ExecutionEventSink.live)
      )
      .run(spec, ExecutionStrategy.Sequential)
  }

}
