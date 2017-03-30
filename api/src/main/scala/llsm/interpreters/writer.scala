package llsm.interpreters

import java.lang.{Integer, Math}
import java.nio.file.{Files, Path, Paths}
import java.util.ArrayList
import scala.collection.JavaConverters._
//import scala.util.{Failure, Success, Try}

import bdv.export.{ProposeMipmaps, ProgressWriterConsole}
import bdv.export.WriteSequenceToHdf5
import bdv.export.WriteSequenceToHdf5.{AfterEachPlane, DefaultLoopbackHeuristic}
import bdv.img.hdf5.{Hdf5ImageLoader, Partition}
import bdv.spimdata.{SequenceDescriptionMinimal, SpimDataMinimal, XmlIoSpimDataMinimal}
import cats.{~>, MonadError}
import cats.implicits._
import cats.free.Free
import ch.systemsx.cisd.hdf5.HDF5Factory
import io.scif.{ ImageMetadata, Metadata, SCIFIO, Writer }
import io.scif.config.SCIFIOConfig
import io.scif.codec.CompressionType
import io.scif.img.{ImgSaver, SCIFIOImgPlus}
import llsm.Deskew
import llsm.algebras.{ImgWriterAPI, ImgWriterF, LowWriterAPI, LowWriterF, WriteSCIFIO, WriteHDF5}
import llsm.fp._
import llsm.io.{LLSMImg, LLSMStack}
import llsm.io.metadata.{AggregatedMeta, FileMetadata, MetadataUtils}
import mpicbg.spim.data.generic.sequence.BasicViewSetup
import mpicbg.spim.data.registration.{ViewRegistration, ViewRegistrations}
import mpicbg.spim.data.sequence.{Channel, FinalVoxelDimensions, TimePoint, TimePoints}
import net.imagej.ImgPlus
import net.imagej.axis.{Axes, CalibratedAxis}
import net.imglib2.{FinalDimensions, RandomAccessibleInterval}
import net.imglib2.img.ImgView
import net.imglib2.realtransform.AffineTransform3D
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import net.imglib2.view.Views
import org.scijava.Context

sealed trait LLSMWriterError
case class HDF5Error(message: String, err: Throwable) extends LLSMWriterError

trait ImgWriterInterpreters {

  private[this] def xyzDims(meta: ImageMetadata): Either[LLSMWriterError, Tuple3[Long, Long, Long]] = {
    val z: Long = meta.getAxisLength(Axes.Z)
    if (z == 1) {
      val message = "HDF5 writing requires at least a 3D image with XYZ dimensions."
      Either.left(HDF5Error(message, new Exception(message)))
    } else {
      val x: Long = meta.getAxisLength(Axes.X)
      val y: Long = meta.getAxisLength(Axes.Y)
      Either.right((x, y, z))
    }
  }

  private[this] def xyzVoxelSize(meta: ImageMetadata): Either[LLSMWriterError, Tuple4[String, Double, Double, Double]] = {
    val zIndex: Int = meta.getAxisIndex(Axes.Z)
    if (zIndex == -1) {
      val msg = "Z dimension does not appear to be calibrated."
      Either.left(HDF5Error(msg, new Exception(msg)))
    } else {
      val x: CalibratedAxis = meta.getAxis(meta.getAxisIndex(Axes.X))
      val y: CalibratedAxis = meta.getAxis(meta.getAxisIndex(Axes.Y))
      val z: CalibratedAxis = meta.getAxis(zIndex)
      Either.right((x.unit, x.calibratedValue(1), y.calibratedValue(1), z.calibratedValue(1)))
    }
  }

  private[this] def createAffine3D(pw: Double, ph: Double, pd: Double): AffineTransform3D = {
    val sourceTransform = new AffineTransform3D
    sourceTransform.set(pw, 0, 0, 0, 0, ph, 0, 0, 0, 0, pd, 0)
    sourceTransform
  }

  private[this] def removeExtension(name: String): String = {
    val sepIndex = name.lastIndexOf(".") match {
      case -1 => name.length
      case i => i
    }

    name.substring(0, sepIndex)
  }

  private[this] def splitExtension(name: String): Tuple2[String, String] = {
    if (name.endsWith(".ome.tif")) {
      (name.substring(0, name.lastIndexOf(".ome.tif")), "ome.tif")
    } else {
      val sepIndex = name.lastIndexOf(".") match {
        case -1 => name.length
        case i => i
      }

      (name.substring(0, sepIndex), name.substring(sepIndex + 1, name.length))
    }
  }


  def llsmWriter[M[_]](context: Context)(implicit me: MonadError[M, Throwable]): ImgWriterF ~> M =
    new (ImgWriterF ~> M) {
      def apply[B](fa: ImgWriterF[B]): M[B] =
        llsmWriterToLowWriter(fa).foldMap(llsmLowWriter[M](context))
    }

  val llsmWriterToLowWriter: ImgWriterF ~< LowWriterF =
    new (ImgWriterF ~< LowWriterF) {
      def apply[A](fa: ImgWriterF[A]): Free[LowWriterF, A] =
        fa match {
          case ImgWriterAPI.WriteImg(path, img, next) =>
            if (path.toString.endsWith(".h5"))
              LowWriterAPI.writeHDF5(path, img).map(next)
            else
              LowWriterAPI.writeSCIFIO(path, img).map(next)
        }
    }


  def llsmLowWriter[M[_]](context: Context)(implicit M: MonadError[M, Throwable]): LowWriterF ~> M =
    new (LowWriterF ~> M) {
      def apply[B](fa: LowWriterF[B]): M[B] =
        fa match {
          case WriteSCIFIO(path, LLSMImg(img, AggregatedMeta(meta))) => {
            M.catchNonFatal{
              val saver = new ImgSaver(context)
              val conf = new SCIFIOConfig().writerSetSequential(false)
                .writerSetCompression(CompressionType.LZW.getCompression)
                .imgSaverSetWriteRGB(false)

              val axes = meta.getAxes.asScala.toArray

              val imgPlus = meta.getAxisIndex(Axes.CHANNEL) match {
                case -1     =>
                  new ImgPlus[UnsignedShortType](img, meta.getName, axes:_*)
                case i: Int => {
                  val temp = axes(2)
                  axes(2) = axes(i)
                  axes(i) = temp
                  new ImgPlus[UnsignedShortType](
                    ImgView.wrap(Views.permute(img, meta.getAxisIndex(Axes.CHANNEL), 2), img.factory),
                    meta.getName, axes:_*)
                }
              }
              imgPlus.setCompositeChannelCount(1)

              val m = saver.saveImg(path.toString, new SCIFIOImgPlus[UnsignedShortType](imgPlus), 0, conf);
            }
          }
          case WriteSCIFIO(path, LLSMStack(img, fm @ FileMetadata(file, wave, cam, sample, config, im))) =>
            M.catchNonFatal {

              val conf = new SCIFIOConfig()//.writerSetSequential(false)
                .writerSetCompression(CompressionType.LZW.getCompression)
                .imgSaverSetWriteRGB(false)

              val (name, ext) = splitExtension(path.getFileName.toString)
              val outputName = s"${name}_C${file.channel}_T${file.stack}.$ext"
              val outputPath = Paths.get(path.getParent.toString, outputName)

              val scifio = new SCIFIO(context)
              val format = scifio.format().getFormat(path.toString)

              val meta: Metadata = format.createMetadata
              meta.add(MetadataUtils.populateImageMetadata(img, fm))

              val writer: Writer = format.createWriter()
              writer.setMetadata(meta)
              writer.setDest(outputPath.toString)

              val aTypes = meta.get(0).getAxes.asScala.map(a => a.`type`)
              val cals = meta.get(0).getAxes.asScala.map(a => a.calibratedValue(1))

              val imgPlus = new SCIFIOImgPlus(img, outputName, aTypes.toArray, cals.toArray)
              //imgPlus.setMetadata(imeta)

              val saver = new ImgSaver(context)

              val m = saver.saveImg(writer, imgPlus, 0, conf)
            }
          case WriteHDF5(path, LLSMStack(img, FileMetadata(file, wave, cam, sample, config, im))) =>
            M.catchNonFatal {
              if (!Files.exists(path))
                WriterUtils.createHDF5(path)

              val dims = Array.ofDim[Long](img.numDimensions)
              img.dimensions(dims)
              val size = new FinalDimensions(dims:_*)
              val (units, vw, vh, vd) = ("um", config.xVoxelSize, config.yVoxelSize, Deskew.calcZInterval(wave.sPZTInterval, wave.zPZTInterval, sample.angle, config.xVoxelSize))
              val voxelSize = new FinalVoxelDimensions(units, vw, vh, vd)
              val c: Int = wave.channels.size
              val t: Int = wave.nFrames
              val mipmaps = ProposeMipmaps.proposeMipmaps(new BasicViewSetup(0, "", size, voxelSize))
              val transform = createAffine3D(vw, vh, vd)
              val channels: Map[Integer, BasicViewSetup] = (0 until c).map(n => {
                val setup = new BasicViewSetup(n, s"channel ${n + 1}", size, voxelSize)
                setup.setAttribute(new Channel(n + 1))
                new Integer(n) -> setup
              }).toMap
              val timePoints: List[TimePoint] = (0 until t).map(n => new TimePoint(n)).toList
              val lookback = new DefaultLoopbackHeuristic
              val afterEachPlane = new AfterEachPlane() {
                override def afterEachPlane(usedLoopBack: Boolean): Unit = {}
              }
              val partition = new Partition(path.toString,
                timePoints.map(t => new Integer(t.getId) -> new Integer(t.getId)).toMap.asJava,
                channels.map{ case (_, c) => new Integer(c.getId) -> new Integer(c.getId)}.toMap.asJava)
              val cores = Runtime.getRuntime().availableProcessors()
              val numCellCreatorThreads = Math.min(if (cores > 1) cores - 1 else 1, 4)
              val progress = new ProgressWriterConsole

              WriteSequenceToHdf5.writeViewToHdf5PartitionFile(img, partition, file.stack, file.channel, mipmaps, true, true, lookback, afterEachPlane, numCellCreatorThreads, progress)

              val outputDir = path.getParent
              val name: String = s"${removeExtension(path.getFileName.toString)}.xml"
              val seqPath: Path = Paths.get(outputDir.toString, name)

              WriterUtils.writeSeqDescription(path, seqPath, partition, timePoints, channels, transform)
            }
          case WriteHDF5(path, LLSMImg(img, AggregatedMeta(meta))) =>
            // This code is adapted from the WriteSequenceToHdf5 BDV class. We
            // only have a single view, which simplifies things greatly. For
            // this reason we only create a single Partition
            M.catchNonFatal {
              xyzDims(meta).flatMap {
                case (x, y, z) => xyzVoxelSize(meta).map {
                  case (unit, vw, vh, vd) => {
                    val size = new FinalDimensions(x, y, z)
                    val voxelSize = new FinalVoxelDimensions(unit, vw, vh, vd)
                    val c: Int = meta.getAxisLength(Axes.CHANNEL).toInt
                    val t: Int = meta.getAxisLength(Axes.TIME).toInt
                    val mipmaps = ProposeMipmaps.proposeMipmaps(new BasicViewSetup(0, "", size, voxelSize))
                    val transform = createAffine3D(vw, vh, vd)
                    val channels: Map[Integer, BasicViewSetup] = (0 until c).map(n => {
                      val setup = new BasicViewSetup(n, s"channel ${n + 1}", size, voxelSize)
                      setup.setAttribute(new Channel(n + 1))
                      new Integer(n) -> setup
                    }).toMap
                    val timePoints: List[TimePoint] = (0 until t).map(n => new TimePoint(n)).toList
                    val lookback = new DefaultLoopbackHeuristic
                    val afterEachPlane = new AfterEachPlane() {
                      override def afterEachPlane(usedLoopBack: Boolean): Unit = {}
                    }
                    val partition = new Partition(path.toString,
                      timePoints.map(t => new Integer(t.getId) -> new Integer(t.getId)).toMap.asJava,
                      channels.map{ case (i, c) => new Integer(c.getId) -> new Integer(c.getId)}.toMap.asJava)
                    val cores = Runtime.getRuntime().availableProcessors()
                    val numCellCreatorThreads = Math.min(if (cores > 1) cores - 1 else 1, 4)
                    val progress = new ProgressWriterConsole

                    timePoints.foreach {
                      case time => {
                        val tSlice: RandomAccessibleInterval[UnsignedShortType] =
                          if (t > 1)
                            Views.dropSingletonDimensions(Views.hyperSlice(img, img.numDimensions - 1, time.getId.toLong))
                          else
                            img

                        channels.foreach {
                          case (ch, _) => {
                            val im: RandomAccessibleInterval[UnsignedShortType] =
                              if (c > 1)
                                Views.dropSingletonDimensions(Views.hyperSlice(tSlice, tSlice.numDimensions - 1, ch.toLong))
                              else
                                tSlice

                            WriteSequenceToHdf5.writeViewToHdf5PartitionFile(im, partition, time.getId, ch, mipmaps, true, true, lookback, afterEachPlane, numCellCreatorThreads, progress)
                          }
                        }
                      }
                    }

                    val outputDir = path.getParent
                    val name: String = s"${removeExtension(path.getFileName.toString)}.xml"
                    val seqPath: Path = Paths.get(outputDir.toString, name)
                    WriterUtils.writeSeqDescription(path, seqPath, partition, timePoints, channels, transform)
                  }
                }
              } match {
                case Right(_) => ()
                case Left(HDF5Error(m, e)) => throw new Exception(e)
              }
        }
    }

}
}

object WriterUtils {
  def createHDF5(path: Path): Unit = synchronized {
    if (!Files.exists(path)) {
      val w = HDF5Factory.open(path.toFile)
      w.close()
    }
  }

  def writeSeqDescription(
      hdf5Path: Path,
      seqPath: Path,
      partition: Partition,
      timepoints: List[TimePoint],
      channels: Map[Integer, BasicViewSetup],
      transform: AffineTransform3D): Unit = synchronized {
    val parts = new ArrayList[Partition]
    parts.add(partition)

    val hdf5Loader = new Hdf5ImageLoader(hdf5Path.toFile, parts, null, false)
    val seq = new SequenceDescriptionMinimal(
      new TimePoints(timepoints.asJava),
      channels.asJava,
      hdf5Loader,
      null)

    val regs: List[ViewRegistration] = for {
      t <- timepoints
      (i, _) <- channels
    } yield new ViewRegistration(t.getId, i, transform)

    val sdm = new SpimDataMinimal(seqPath.getParent.toFile, seq, new ViewRegistrations(regs.asJava))
    val writer = new XmlIoSpimDataMinimal()
    writer.save(sdm, seqPath.toString)
  }
}
