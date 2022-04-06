package zio.mock.testing

final case class CapturingException[E](captured: E) extends Exception(s"Captured exception: $captured")