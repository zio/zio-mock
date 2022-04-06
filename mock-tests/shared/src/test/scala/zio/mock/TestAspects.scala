package zio.mock

import zio._
import zio.test._
import izumi.reflect.Tag

object TestAspects {

  type ScopeType[R0, E0] = ZIO[Scope with R0, TestFailure[E0], TestSuccess => ZIO[R0, TestFailure[E0], TestSuccess]]

  def withEnv[Env: Tag, R, E](env: ZIO[R, E, ZEnvironment[Env]]) = {
    val scoped: ScopeType[R, E] = {
       (for {
        mockedZEnv <- env
        // Since we may be overriding native services that are not available explicitly in the environment,
        // we need to explicitly override them in a scope.
        _     <- ZEnv.services.locallyScopedWith(_.union(mockedZEnv))
      } yield {
        { success: TestSuccess => ZIO.succeed(success) }
      }).mapError(TestFailure.fail)
    }
    TestAspect.aroundTest[R, E](scoped)
  }

  def withExpectationsAsEnv[Env: Tag, R, E](expectations: Expectation[Env]) = 
    withEnv[Env, R with Scope, E](expectations.build)
}
