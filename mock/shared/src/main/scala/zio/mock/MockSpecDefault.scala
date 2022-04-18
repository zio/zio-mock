package zio.mock

import zio.test.ZIOSpecDefault
import zio.{Chunk, ZIOAppArgs}
import zio.test.{TestAspectAtLeastR, TestEnvironment}

abstract class MockSpecDefault extends ZIOSpecDefault {

  override def aspects = super.aspects :+ MockReporter()

}
