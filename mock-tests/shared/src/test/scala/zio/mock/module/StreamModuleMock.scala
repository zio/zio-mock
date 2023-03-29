package zio.mock.module

import zio.mock.{Mock, Proxy}
import zio.stream.{ZSink, ZStream}
import zio.{URLayer, Unsafe, ZIO, ZLayer}

/** Example module used for testing ZIO Mock framework.
  */
object StreamModuleMock extends Mock[StreamModule] {

  object Sink   extends Sink[Any, String, Int, Nothing, List[Int]]
  object Stream extends Stream[Any, String, Int]

  val compose: URLayer[Proxy, StreamModule] =
    ZLayer.fromZIO(
      ZIO
        .service[Proxy]
        .flatMap { proxy =>
          withRuntime[Proxy, StreamModule] { rts =>
            ZIO.succeed {
              new StreamModule {
                def sink(a: Int) =
                  Unsafe.unsafe { implicit u =>
                    val t1: ZIO[Any, String, ZSink[Any, String, Int, Nothing, List[Int]]] = proxy(Sink, a)
                    rts.unsafe
                      .run(t1.catchAll(error => ZIO.succeed(ZSink.fail[String](error))))
                      .getOrThrowFiberFailure()
                  }

                def stream(a: Int) = Unsafe.unsafe { implicit u =>
                  val t1: ZIO[Any, Nothing, ZStream[Any, String, Int]] = proxy(Stream, a)
                  rts.unsafe.run(t1).getOrThrowFiberFailure()
                }
              }
            }
          }
        }
    )
}
