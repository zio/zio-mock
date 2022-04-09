package zio.mock

import zio._

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

}
