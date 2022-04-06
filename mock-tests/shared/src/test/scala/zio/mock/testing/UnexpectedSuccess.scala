package zio.mock.testing

import zio._

final case class UnexpectedSuccess(something: Any)
    extends Exception(s"Test succeeded unexpectedly with '$something' (which is a '${something.getClass}'').")

object UnexpectedSuccess {

  def makeZIO(something: Any) = ZIO.succeed(UnexpectedSuccess(something))
}
