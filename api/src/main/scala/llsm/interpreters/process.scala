package llsm.interpreters

import cats.{~>, MonadError}
import llsm.Deskew
import llsm.algebras.{ProcessAPI, ProcessF}
import llsm.io._
import llsm.io.{ImgUtils, LLSMStack}
import net.imglib2.img.ImgView

trait ProcessInterpreters {
  def processCompiler[M[_]](
    implicit M: MonadError[M, Throwable]
  ): ProcessF ~> M =
    new (ProcessF ~> M) {
      def apply[A](fa: ProcessF[A]): M[A] =
        fa match {
          case ProcessAPI.AggregateImgs(imgs, next) =>
            M.pure(next(ImgUtils.aggregateImgs(imgs)))
          case ProcessAPI.DeskewImg(LLSMStack(img, meta), sDim, rDim, shearFactor, interpolation, next) =>
            M.pure(next(LLSMStack(ImgView.wrap(Deskew.deskew(sDim, rDim, shearFactor, interpolation)(img), img.factory), meta)))
          case ProcessAPI.Deconvolve(LLSMStack(img, meta), LLSMStack(psf, _), maxIterations, next) => ???
        }
    }
}
