package zio.mock

import zio._
import zio.test._
import izumi.reflect.Tag

object TestAspects {

  type ScopeType[R0, E0] = ZIO[Scope with R0, TestFailure[E0], TestSuccess => ZIO[R0, TestFailure[E0], TestSuccess]]

  def withEnv[Env: Tag, R, E](env: ZIO[R, E, ZEnvironment[Env]]) = {
    // scalafmt was corrupting this code.
    // format: off
    val scoped: ScopeType[R, E] = {
       (for {
        mockedZEnv <- env
        // Since we may be overriding native services that are not available explicitly in the environment,
        // we need to explicitly override them in a scope.

        // TODO: This was originally `ZEnv.services.locallyScopedWith(_.union(mockedZEnv))`. Not sure what the 2.0.0 equivalent is
        _     <- DefaultServices.currentServices.locallyScopedWith(_.add(mockedZEnv))
      } yield {
        { (success: TestSuccess) => ZIO.succeed(success) }
      }).mapError(TestFailure.fail)
    }
    // format: on
    TestAspect.aroundTest[R, E](scoped)
  }

  def withExpectationsAsEnv[Env: Tag, R, E](expectations: Expectation[Env]) =
    withEnv[Env, R with Scope, E](expectations.build)
}
