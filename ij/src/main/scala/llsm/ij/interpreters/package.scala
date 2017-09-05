package llsm.ij

import bdv.util.{
  AxisOrder,
  Bdv,
  BdvFunctions
}
import cats._
import cats.free.Free
import cats.implicits._
import ij.ImagePlus
import ij.measure.Calibration
import llsm.algebras.{
  ImgReaderF,
  LoggingAPI,
  LoggingF,
  MetadataF,
  ProgressAPI,
  ProgressF,
  VisualiseF,
  VisualiseAPI
}
import llsm.fp._
import llsm.interpreters._
import llsm.io.metadata.ConfigurableMetadata
import net.imagej.axis.Axes
import net.imglib2.img.ImgFactory
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import net.imglib2.view.Views
import org.scijava.Context
import org.scijava.app.StatusService
import org.scijava.log.LogService

package object interpreters {

  def ijProgress[M[_]](status: StatusService)(implicit M: MonadError[M, Throwable]): ProgressF ~> M =
    new (ProgressF ~> M) {
      def apply[A](fa: ProgressF[A]): M[A] =
        fa match {
          case ProgressAPI.Progress(value: Int, max: Int) =>
            M.catchNonFatal(status.showProgress(value, max))
          case ProgressAPI.Status(message: String) =>
            M.catchNonFatal(status.showStatus(message))
        }
    }

  def ijLogging[M[_]](log: LogService)(implicit M: MonadError[M, Throwable]): LoggingF ~> M =
    new (LoggingF ~> M) {
      def apply[A](fa: LoggingF[A]): M[A] =
        fa match {
          case LoggingAPI.Info(msg) => M.catchNonFatal(log.info(msg))
          case LoggingAPI.InfoCause(msg, cause) => M.catchNonFatal(log.info(msg, cause))
          case LoggingAPI.Warn(msg) => M.catchNonFatal(log.warn(msg))
          case LoggingAPI.WarnCause(msg, cause) => M.catchNonFatal(log.warn(msg, cause))
          case LoggingAPI.Debug(msg) => M.catchNonFatal(log.debug(msg))
          case LoggingAPI.DebugCause(msg, cause) => M.catchNonFatal(log.debug(msg, cause))
          case LoggingAPI.Error(msg) => M.catchNonFatal(log.error(msg))
          case LoggingAPI.ErrorCause(msg, cause) => M.catchNonFatal(log.error(msg, cause))
          case LoggingAPI.Trace(msg) => M.catchNonFatal(log.trace(msg))
          case LoggingAPI.TraceCause(msg, cause) => M.catchNonFatal(log.trace(msg, cause))
        }
    }



  def ijMetadataReader[M[_]](
    config: ConfigurableMetadata,
    context: Context,
    log: LogService
  )(implicit
    M: MonadError[M, Throwable]
  ): MetadataF ~> M =
    new (MetadataF ~> M) {
      def apply[A](fa: MetadataF[A]): M[A] =
        for {
          _ <- metadataLogging(fa).unhalt.foldMap(ijLogging[M](log))
          m <- basicMetadataReader[M](config, context)(M)(fa)
        } yield m
    }


  def ijImgReader[M[_]](
    context: Context,
    factory: ImgFactory[UnsignedShortType],
    log: LogService
  )(implicit
    M: MonadError[M, Throwable]
  ): ImgReaderF ~> M =
    new (ImgReaderF ~> M) {
      def apply[A](fa: ImgReaderF[A]): M[A] =
        for {
          _ <- readerLogging(fa).unhalt.foldMap(ijLogging[M](log))
          m <- scifioReader[M](context, factory)(M)(fa)
        } yield m
    }

  // Visualisation Interpreters
  def visInterpreter(viewer: Viewer): VisualiseF ~< VisualiseIJF =
    new (VisualiseF ~< VisualiseIJF) {
      def apply[A](f: VisualiseF[A]): Free[VisualiseIJF, A] = f match {
        case VisualiseAPI.Show(img, next) => viewer match {
          case HyperStack => VisualiseIJF.showHyper(img).map(next)
          case BigDataViewer => VisualiseIJF.showBDV(img).map(next)
        }
      }
    }

  def ijVisCompiler[M[_]](
    implicit
    M: MonadError[M, Throwable]
  ): VisualiseIJF ~> M =
    new (VisualiseIJF ~> M) {
      def apply[A](f: VisualiseIJF[A]): M[A] = f match {
        case ShowHyper(img) => {
          val imeta = img.getImageMetadata
          val cIndex: Option[Int] = imeta.getAxisIndex(Axes.CHANNEL) match {
            case -1     => None
            case i: Int => Some(i)
          }

          val imgDims = Array.ofDim[Long](img.numDimensions)
          img.dimensions(imgDims)

          val out: ImagePlus = cIndex match {
            case Some(i) =>
              ImageJFunctions.wrap(Views.permute(img, i, 2),
                s"${imeta.getName}_deskewed")
              case None =>
                ImageJFunctions.wrap(img, s"${imeta.getName}_deskewed")
          }

          val cal = new Calibration(out)
          cal.setUnit("um")
          cal.pixelWidth = imeta.getAxis(Axes.X).calibratedValue(1)
          cal.pixelHeight = imeta.getAxis(Axes.Y).calibratedValue(1)
          cal.pixelDepth = imeta.getAxis(Axes.Z).calibratedValue(1)

          if (imeta.getAxisLength(Axes.TIME) > 1) {
            cal.setTimeUnit("ms")
            cal.frameInterval = imeta.getAxis(Axes.TIME).calibratedValue(1)
          }

          out.setCalibration(cal)
          out.setDimensions(
            imeta.getAxisLength(Axes.CHANNEL).toInt,
            imeta.getAxisLength(Axes.Z).toInt,
            imeta.getAxisLength(Axes.TIME).toInt
          )
          M.pure(out.show())
        }
        case ShowBDV(img) => {
          val imeta = img.getImageMetadata

          val axisOrder = if (imeta.getAxisLength(Axes.CHANNEL) > 1 &&
            imeta.getAxisLength(Axes.TIME) > 1) {
              AxisOrder.XYZCT
            } else if (imeta.getAxisLength(Axes.CHANNEL) > 1) {
              AxisOrder.XYZC
            } else if (imeta.getAxisLength(Axes.TIME) > 1) {
              AxisOrder.XYZT
            } else {
              AxisOrder.XYZ
            }

          val bdvOpts = Bdv.options()
            .sourceTransform(
              imeta.getAxis(Axes.X).calibratedValue(1),
              imeta.getAxis(Axes.Y).calibratedValue(1),
              imeta.getAxis(Axes.Z).calibratedValue(1)
            )
            .axisOrder(axisOrder)

          M.pure{
            BdvFunctions.show(img, s"${imeta.getName}_deskewed", bdvOpts)
            ()
          }
        }
      }
    }

    def ijVis[M[_]](viewer: Viewer)(
      implicit
      M: MonadError[M, Throwable]
    ): VisualiseF ~> M =
      new (VisualiseF ~> M) {
        def apply[A](fa: VisualiseF[A]): M[A] =
          visInterpreter(viewer)(fa).foldMap(ijVisCompiler[M])
      }
}
