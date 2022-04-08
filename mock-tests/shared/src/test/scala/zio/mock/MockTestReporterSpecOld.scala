package zio.mock

import zio.test.TestAspect._

object MockTestReporterSpecOld extends ZIOBaseSpec {

  def spec =
    suite("MockTestReporterSpecOld")(
      suite("reports")(
        // test("a successful test") {
        //   runLog(test1).map(res => assertTrue(test1Expected == res))
        // },
        // test("a failed test") {
        //   runLog(test3).map(res => test3Expected.map(expected => assertTrue(res.contains(expected))).reduce(_ && _))
        // },
        // test("an error in a test") {
        //   runLog(test4).map(log => assertTrue(log.contains("Test 4 Fail")))
        // },
        // test("successful test suite") {
        //   runLog(suite1).map(res => suite1Expected.map(expected => assertTrue(res.contains(expected))).reduce(_ && _))
        // },
        // test("failed test suite") {
        //   runLog(suite2).map(res => suite2Expected.map(expected => assertTrue(res.contains(expected))).reduce(_ && _))
        // },
        // test("multiple test suites") {
        //   runLog(suite3).map(res => suite3Expected.map(expected => assertTrue(res.contains(expected))).reduce(_ && _))
        // },
        // test("empty test suite") {
        //   runLog(suite4).map(res => suite4Expected.map(expected => assertTrue(res.contains(expected))).reduce(_ && _))
        // },
        // test("failure of simple assertion") {
        //   runLog(test5).map(res => test5Expected.map(expected => assertTrue(res.contains(expected))).reduce(_ && _))
        // },
        // test("multiple nested failures") {
        //   runLog(test6).map(res => test6Expected.map(expected => assertTrue(res.contains(expected))).reduce(_ && _))
        // },
        // test("labeled failures") {
        //   runLog(test7).map(res => test7Expected.map(expected => assertTrue(res.contains(expected))).reduce(_ && _))
        // },
        // test("labeled failures for assertTrue") {
        //   for {
        //     log <- runLog(test9)
        //   } yield assertTrue(log.contains("""?? "third""""), log.contains("""?? "fourth""""))
        // },
        // test("negated failures") {
        //   runLog(test8).map(res => test8Expected.map(expected => assertTrue(res.contains(expected))).reduce(_ && _))
        // }
      )
      // test("correctly reports a successful test") {
      //   // assertM(runLog(test1))(equalTo(test1Expected.mkString + reportStats(1, 0, 0)))
      //   // runLog(test1).map { results =>
      //   //   println(s"::: OUTPUT '$results'")
      //   //   assertTrue(
      //   //     results == test1Expected.mkString + reportStats(1, 0, 0)
      //   //   )
      //   // }
      //             runLog(test1).map{ res =>

      //               println(s"::: OUTPUT '$res'")
      //               assertTrue(test1Expected == res)}
      // },
      // test("correctly reports a failed test") {
      //   assertM(runLog(test3))(equalTo(test3Expected.mkString + "\n" + reportStats(0, 0, 1)))
      // } @@ ignore,
      // test("correctly reports an error in a test") {
      //   for {
      //     log <- runLog(test4)
      //   } yield assertTrue(log.contains("Test 4 Fail"))
      // } @@ ignore,
      // test("correctly reports successful test suite") {
      //   assertM(runLog(suite1))(equalTo(suite1Expected.mkString + reportStats(2, 0, 0)))
      // } @@ ignore,
      // test("correctly reports failed test suite") {
      //   assertM(runLog(suite2))(equalTo(suite2Expected.mkString + "\n" + reportStats(2, 0, 1)))
      // } @@ ignore,
      // test("correctly reports multiple test suites") {
      //   assertM(runLog(suite3))(equalTo(suite3Expected.mkString + "\n" + reportStats(4, 0, 2)))
      // } @@ ignore,
      // test("correctly reports empty test suite") {
      //   assertM(runLog(suite4))(equalTo(suite4Expected.mkString + "\n" + reportStats(2, 0, 1)))
      // } @@ ignore,
      // test("correctly reports failure of simple assertion") {
      //   assertM(runLog(test5))(equalTo(test5Expected.mkString + "\n" + reportStats(0, 0, 1)))
      // } @@ ignore,
      // test("correctly reports multiple nested failures") {
      //   assertM(runLog(test6))(equalTo(test6Expected.mkString + "\n" + reportStats(0, 0, 1)))
      // } @@ ignore,
      // test("correctly reports labeled failures") {
      //   assertM(runLog(test7))(equalTo(test7Expected.mkString + "\n" + reportStats(0, 0, 1)))
      // } @@ ignore,
      // test("correctly reports negated failures") {
      //   assertM(runLog(test8))(equalTo(test8Expected.mkString + "\n" + reportStats(0, 0, 1)))
      // } @@ ignore,
      // test("correctly reports mock failure of invalid call") {
      //   runLog(mock1).map(str => assertTrue(str == mock1Expected.mkString + reportStats(0, 0, 1)))
      // } @@ ignore,
      // test("correctly reports mock failure of unmet expectations") {
      //   runLog(mock2).map(str => assertTrue(str == mock2Expected.mkString + reportStats(0, 0, 1)))
      // } @@ ignore,
      // test("correctly reports mock failure of unexpected call") {
      //   assertM(runLog(mock3))(equalTo(mock3Expected.mkString + reportStats(0, 0, 1)))
      // } @@ ignore,
      // test("correctly reports mock failure of invalid range") {
      //   assertM(runLog(mock4))(equalTo(mock4Expected.mkString + reportStats(0, 0, 1)))
      // } @@ ignore
    ) @@ silent
}
