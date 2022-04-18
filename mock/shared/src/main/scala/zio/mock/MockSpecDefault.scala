package zio.mock

import zio.test.{TestAspectAtLeastR, TestEnvironment, ZIOSpecDefault}
import zio.{Chunk, ZIOAppArgs}

abstract class MockSpecDefault extends ZIOSpecDefault {

  override def aspects: Chunk[TestAspectAtLeastR[Environment with TestEnvironment with ZIOAppArgs]] =
    super.aspects :+ MockReporter()

}
