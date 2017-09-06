package llsm.interpreters

import cats.{~>, MonadError}
import llsm.{
  Deskew,
  ImgUtils
}
import llsm.algebras.{ProcessAPI, ProcessF}
import llsm.io._
import llsm.io.LLSMImg
import net.imglib2.img.ImgView

trait ProcessInterpreters {
  def processInterpreter[M[_]](
    implicit M: MonadError[M, Throwable]
  ): ProcessF ~> M =
    new (ProcessF ~> M) {
      def apply[A](fa: ProcessF[A]): M[A] =
        fa match {
          case ProcessAPI.Aggregate(imgs, next) => ImgUtils.aggregateImgs(imgs) match {
            case Some(img) => M.pure(next(img))
            case None      => M.raiseError(new Exception("Failed to aggregate LLSMImgs to a SCIFIOImgPlus."))
          }
          case ProcessAPI.DeskewImg(LLSMImg(img, meta), sDim, rDim, shearFactor, interpolation, next) =>
            M.pure(next(LLSMImg(ImgView.wrap(Deskew.deskew(sDim, rDim, shearFactor, interpolation)(img), img.factory), meta)))
        }
    }
}
