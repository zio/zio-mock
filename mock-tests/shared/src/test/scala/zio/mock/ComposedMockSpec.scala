package zio.mock

import zio._
import zio.test._

object ComposedMockSpec extends ZIOBaseSpec {

  import Assertion._
  import Expectation._

  def spec = suite("ComposedMockSpec")(
    suite("mocking composed environments")(
      test("Console with Clock") {
        for {
          time <- Clock.nanoTime
          _    <- Console.printLine(time.toString)
        } yield (assertTrue(true))

      } @@ TestAspects.withExpectationsAsEnv(
        MockClock.NanoTime(value(42L)) ++
          MockConsole.PrintLine(equalTo("42"), unit)
      ),
      test("Random with Clock with System with Console") {

        for {
          n <- Random.nextInt
          _ <- Clock.sleep(n.seconds)
          v <- System.property("foo")
          _ <- Console.printLine(v.toString)
        } yield (assertTrue(true))
      } @@ TestAspects.withExpectationsAsEnv(
        MockRandom.NextInt(value(42)) ++
          MockClock.Sleep(equalTo(42.seconds)) ++
          MockSystem.Property(equalTo("foo"), value(None)) ++
          MockConsole.PrintLine(equalTo("None"), unit)
      )
    )
  )
}
