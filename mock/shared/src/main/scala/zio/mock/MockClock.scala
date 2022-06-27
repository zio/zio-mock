/*
 * Copyright 2019-2022 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio.mock

import zio.internal.stacktracer.Tracer
import zio.stacktracer.TracingImplicits.disableAutoTrace
import zio.{UIO, _}

import java.time
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object MockClock extends Mock[Clock] {

  object CurrentTime     extends Effect[ChronoUnit, Nothing, Long]
  object CurrentDateTime extends Effect[Unit, Nothing, OffsetDateTime]
  object Instant         extends Effect[Unit, Nothing, java.time.Instant]
  object LocalDateTime   extends Effect[Unit, Nothing, java.time.LocalDateTime]
  object NanoTime        extends Effect[Unit, Nothing, Long]
  object Scheduler       extends Effect[Unit, Nothing, Scheduler]
  object Sleep           extends Effect[Duration, Nothing, Unit]
  object JavaClock       extends Effect[Unit, Nothing, time.Clock]

  val compose: URLayer[Proxy, Clock] = {
    implicit val trace = Tracer.newTrace
    ZLayer.fromZIO(
      ZIO
        .service[Proxy]
        .map { proxy =>
          new Clock {

            def currentTime(unit: => TimeUnit)(implicit trace: Trace): UIO[Long]                     = ??? // proxy(CurrentTime, unit)
            def currentTime(unit: => ChronoUnit)(implicit trace: Trace, d: DummyImplicit): UIO[Long] =
              ??? // proxy(ChronoUnit, unit)
            def currentDateTime(implicit trace: Trace): UIO[OffsetDateTime]            = proxy(CurrentDateTime)
            def nanoTime(implicit trace: Trace): UIO[Long]                             = proxy(NanoTime)
            def scheduler(implicit trace: Trace): UIO[Scheduler]                       = proxy(Scheduler)
            def sleep(duration: => Duration)(implicit trace: Trace): UIO[Unit]         = proxy(Sleep, duration)
            def instant(implicit trace: Trace): zio.UIO[java.time.Instant]             = proxy(Instant)
            def localDateTime(implicit trace: Trace): zio.UIO[java.time.LocalDateTime] = proxy(LocalDateTime)
            def javaClock(implicit trace: zio.Trace): UIO[time.Clock]                  = proxy(JavaClock)

          }
        }
    )
  }
}
