package zio.mock

import zio.Chunk
import zio.test.{TestAspectAtLeastR, TestEnvironment, ZIOSpecDefault}

abstract class MockSpecDefault extends ZIOSpecDefault {

  override def aspects: Chunk[TestAspectAtLeastR[TestEnvironment]] =
    super.aspects :+ MockReporter()

}
