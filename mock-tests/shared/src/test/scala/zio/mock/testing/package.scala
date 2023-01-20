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

  def executeSpec[E](spec: Spec[Scope, E], showSpecOutput: Boolean = false) = {
    // environment1: ZEnvironment[ZIOAppArgs with Scope] = castedRuntime.environment
    // sharedLayer: ZLayer[Any, Nothing, Environment with ExecutionEventSink] =
    //   ZLayer.succeedEnvironment(castedRuntime.environment)

    val testOutput = if (showSpecOutput) TestRunner.defaultBootstrap else ZLayer.succeed(SilentTestOutput)
    val layer0     = testEnvironment ++ Scope.default ++ ZIOAppArgs.empty
    val layer1     = TestLogger.fromConsole(
      Console.ConsoleLive
    ) >>> testOutput >>> ExecutionEventSink.live

    // perTestLayer = (ZLayer.succeedEnvironment(environment1) ++ ZEnv.live) >>> (TestEnvironment.live ++ ZLayer
    //                  .environment[Scope] ++ ZLayer.environment[ZIOAppArgs])
    // executionEventSinkLayer = sharedLayer

    TestExecutor
      .default(
        layer0,
        layer0,
        layer1,
        _ => ZIO.unit
      )
      .run("fqn", spec, ExecutionStrategy.Sequential)
  }

}
