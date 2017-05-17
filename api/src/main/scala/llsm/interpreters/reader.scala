package llsm.interpreters

import cats.{ApplicativeError, ~>}
import cats.free.Free
import io.scif.config.SCIFIOConfig
import io.scif.img.ImgOpener
import llsm.algebras.{ImgReaderAPI, ImgReaderF, LoggingAPI, LoggingF}
import llsm.fp._
import llsm.io.LLSMStack
import net.imglib2.img.ImgFactory
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import org.scijava.Context

trait ImgReaderInterpreters {

  /** Returns an SCIFIO based interpreter for the [[llsm.algebras.ImgReaderAPI]]
   *
   *  Interprets the [[llsm.algebras.ImgReaderAPI]] DSL using SCIFIO to
   *  implement reading functionality and captures the result in a specified
   *  container M that has an instance of MonadError.
   *
   *  @param context scijava org.scijava.Context
   *  @tparam M return type that needs an instance of cats.MonadError
   */
  def scifioReader[M[_]](
    context: Context,
    factory: ImgFactory[UnsignedShortType]
  )(implicit
    M: ApplicativeError[M, Throwable]
  ): ImgReaderF ~> M =
    new (ImgReaderF ~> M) {
      def apply[A](fa: ImgReaderF[A]): M[A] =
        fa match {
          case ImgReaderAPI.ReadImg(path, meta, next) => {
            M.pure {
              val imgOpener = new ImgOpener(context)

              next(
                LLSMStack(
                  imgOpener.openImgs(
                    path.toString,
                    factory,
                    new UnsignedShortType).get(0),
                  meta)
              )
            }
          }
      }
  }

  /** Logging for ImgReader DSL
   *
   *  Defines ImgReader DSL in terms of Logging DSL, which adds the side effect of
   *  logging to each call in the ImgReader DSL. The intention is for this to
   *  be composed with a ImgReader interpreter to define a new interpreter that
   *  logs as well.
   */
  val readerLogging: ImgReaderF ~< Halt[LoggingF, ?] =
    new (ImgReaderF ~< Halt[LoggingF, ?]) {
      def apply[A](fa: ImgReaderF[A]): Free[Halt[LoggingF, ?], A] =
        fa match {
          case ImgReaderAPI.ReadImg(path, meta, _) =>
            Free.liftF[Halt[LoggingF, ?], A](
              LoggingAPI[LoggingF].debug(s"Reading image: $path")
            )
        }
    }
}
