package zio.mock

import zio._
import zio.mock.module.T22
import zio.test.{Assertion, Live, Spec, assertZIO, test}
import zio.test.MockUtils._

trait MockSpecUtils[R] {

  lazy val intTuple22: T22[Int] =
    (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22)

  private[mock] def testValue[E, A](name: String)(
      mock: ULayer[R],
      app: ZIO[R, E, A],
      check: Assertion[A]
  )(implicit trace: Trace): Spec[Any, E] = test(name) {
    val result = ZIO.scoped[Any](mock.build.flatMap(app.provideEnvironment(_)))
    assertZIO(result)(check)
  }

  private[mock] def testError[E, A](name: String)(
      mock: ULayer[R],
      app: ZIO[R, E, A],
      check: Assertion[E]
  )(implicit trace: Trace): Spec[Any, A] = test(name) {
    val result = ZIO.scoped[Any](mock.build.flatMap(app.flip.provideEnvironment(_)))
    assertZIO(result)(check)
  }

  private[mock] def testValueTimeboxed[E, A](name: String)(duration: Duration)(
      mock: ULayer[R],
      app: ZIO[R, E, A],
      check: Assertion[Option[A]]
  )(implicit trace: Trace): Spec[Live, E] = test(name) {
    val t1: ZIO[Scope, E, A] = mock.build.flatMap(app.provideEnvironment(_))
    val t2: ZIO[Any, E, A]   = ZIO.scoped(t1)
    val result               =
      Live.live {
        t2.timeout(duration)
      }

    assertZIO(result)(check)
  }

  private[mock] def testDied[E, A](name: String)(
      mock: ULayer[R],
      app: ZIO[R, E, A],
      check: Assertion[Throwable]
  )(implicit trace: Trace): Spec[Any, Any] = test(name) {

    val result: IO[Any, Throwable] =
      swapFailure(
        ZIO
          .scoped {
            mock.build
              .flatMap(app.provideEnvironment(_))
          }
      )

    assertZIO(result)(check)
  }

}
