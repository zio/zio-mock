package zio.mock

import zio._
import zio.internal.stacktracer.Tracer
import zio.internal.stacktracer.Tracer.{instance, newTrace}
import zio.stacktracer.TracingImplicits.disableAutoTrace
import zio.test._

abstract class MockSpecDefault extends MockSpec[TestEnvironment] {

  override val layer: ZLayer[ZIOAppArgs, Any, TestEnvironment] = {
    implicit val trace: zio.ZTraceElement = Tracer.newTrace
    zio.ZEnv.live >>> TestEnvironment.live
  }

  def spec: ZSpec[TestEnvironment, Any]
}
