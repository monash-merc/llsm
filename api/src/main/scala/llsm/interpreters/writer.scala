package llsm.interpreters

import java.lang.{Integer, Math}
import java.nio.file.{
  Files,
  FileSystems,
  Path,
  Paths
}
import java.util.{ ArrayList, UUID }
import java.util.stream.Collectors

import scala.collection.JavaConverters._

import bdv.export.{ProposeMipmaps, ProgressWriterConsole}
import bdv.export.WriteSequenceToHdf5
import bdv.export.WriteSequenceToHdf5.{AfterEachPlane, DefaultLoopbackHeuristic}
import bdv.img.hdf5.{Hdf5ImageLoader, Partition}
import bdv.spimdata.{
  SequenceDescriptionMinimal,
  SpimDataMinimal,
  XmlIoSpimDataMinimal
}
import cats.{~>, MonadError}
import cats.free.Free
import ch.systemsx.cisd.hdf5.HDF5Factory
import io.scif.{
  DefaultMetadata,
  SCIFIO
}
import io.scif.config.SCIFIOConfig
import io.scif.codec.CompressionType
import io.scif.img.{ImgSaver, SCIFIOImgPlus}
import io.scif.formats.TIFFFormat
import llsm.Deskew
import llsm.algebras.{
  ImgWriterAPI,
  ImgWriterF,
  LowWriterAPI,
  LowWriterF,
  WriteOMETIFF,
  WriteHDF5,
  WriteError
}
import llsm.formats.OMETIFFCustom
import llsm.fp._
import llsm.io.LLSMImg
import llsm.io.metadata.{
  FileMetadata,
  MetadataUtils
}
import loci.formats.ome.OMEXMLMetadataImpl
import mpicbg.spim.data.generic.sequence.BasicViewSetup
import mpicbg.spim.data.registration.{ViewRegistration, ViewRegistrations}
import mpicbg.spim.data.sequence.{
  Channel,
  FinalVoxelDimensions,
  TimePoint,
  TimePoints
}
import net.imglib2.FinalDimensions
import net.imglib2.realtransform.AffineTransform3D
import org.scijava.Context

sealed trait LLSMWriterError
case class HDF5Error(message: String, err: Throwable) extends LLSMWriterError

trait ImgWriterInterpreters {

  private[this] def createAffine3D(pw: Double, ph: Double, pd: Double): AffineTransform3D = {
    val sourceTransform = new AffineTransform3D
    sourceTransform.set(pw, 0, 0, 0, 0, ph, 0, 0, 0, 0, pd, 0)
    sourceTransform
  }

  def imgWriterInterpreter[M[_]](
    context: Context
  )(
    implicit
    me: MonadError[M, Throwable]
  ): ImgWriterF ~> M =
    new (ImgWriterF ~> M) {
      def apply[B](fa: ImgWriterF[B]): M[B] =
        imgWriterToLowWriterTranslator(fa)
          .foldMap(imgLowWriterInterpreter[M](context))
    }

  val imgWriterToLowWriterTranslator: ImgWriterF ~< LowWriterF =
    new (ImgWriterF ~< LowWriterF) {
      def apply[A](fa: ImgWriterF[A]): Free[LowWriterF, A] =
        fa match {
          case ImgWriterAPI.WriteImg(path, img, next) =>
            if (path.toString.endsWith(".h5"))
              LowWriterAPI.writeHDF5(path, img).map(next)
            else if (path.toString.endsWith(".ome.tif"))
              LowWriterAPI.writeOMETIFF(path, img).map(next)
            else LowWriterAPI.writeError(new Exception("Unsupported output format. Only .h5 and .ome.tif are supported at this time."))
                  .map(next)
        }
    }


  def imgLowWriterInterpreter[M[_]](context: Context)(implicit M: MonadError[M, Throwable]): LowWriterF ~> M =
    new (LowWriterF ~> M) {
      def apply[B](fa: LowWriterF[B]): M[B] =
        fa match {
          case WriteOMETIFF(path, limg @ LLSMImg(img, fm @ FileMetadata(file, wave@_, cam@_, sample@_, config@_))) => {
            val conf = new SCIFIOConfig()
              .writerSetCompression(CompressionType.LZW.getCompression)
              .imgSaverSetWriteRGB(false)

            val (name, ext) = WriterUtils.splitExtension(path.getFileName.toString)
            val outputName = s"${name}_C${file.channel}_T${file.stack}.$ext"
            val outputPath = Paths.get(path.getParent.toString, outputName)

            val scifio = new SCIFIO(context)

            val omexml = new OMEXMLMetadataImpl()
            omexml.setUUID(file.id.toString)
            omexml.setBinaryOnlyMetadataFile(s"${name}.companion.ome")
            omexml.setBinaryOnlyUUID(UUID.nameUUIDFromBytes(name.getBytes).toString)

            // Create ImageMetadata
            val imeta = MetadataUtils.createImageMetadata(limg)
            val meta = new TIFFFormat.Metadata()
            val success = scifio.translator().translate(new DefaultMetadata(List(imeta).asJava), meta, false)

            if (success) {
              val writer = new OMETIFFCustom.Writer()
              writer.setContext(context)
              writer.setMetadata(meta)
              writer.setDest(outputPath.toString, 0)
              writer.setOMEXMLMeta(omexml)

              val imgPlus = new SCIFIOImgPlus(img, outputName)

              val saver = new ImgSaver(context)

              M.map(M.catchNonFatal(saver.saveImg(writer, imgPlus, conf)))(_ => {
                val filenameMeta = file.copy(name = outputName)
                fm.copy(filename = filenameMeta)
              })
            } else M.raiseError(new Exception("Unable to translate ImageMetadata to OMEXML metadata."))
          }
          case WriteHDF5(path, LLSMImg(img, meta @ FileMetadata(file, wave, cam@_, sample, config))) =>
            M.catchNonFatal {
              if (!Files.exists(path))
                WriterUtils.createHDF5(path)

              val dims = Array.ofDim[Long](img.numDimensions)
              img.dimensions(dims)
              val size = new FinalDimensions(dims:_*)
              val (units, vw, vh, vd) = (
                "um",
                config.xVoxelSize,
                config.yVoxelSize,
                Deskew.calcZInterval(wave.sPZTInterval, wave.zPZTInterval, sample.angle)
              )
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
              val name: String = s"${WriterUtils.removeExtension(path.getFileName.toString)}.xml"
              val seqPath: Path = Paths.get(outputDir.toString, name)

              WriterUtils.writeSeqDescription(path, seqPath, partition, timePoints, channels, transform)

              val filenameMeta = file.copy(name = path.getFileName.toString)
              meta.copy(filename = filenameMeta)
            }
            case WriteError(t) => M.raiseError(t)
    }

  }
}

object WriterUtils {
  def removeExtension(name: String): String = {
    val sepIndex = name.lastIndexOf(".") match {
      case -1 => name.length
      case i => i
    }

    name.substring(0, sepIndex)
  }

  def splitExtension(name: String): Tuple2[String, String] = {
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

  def outputExists(outputPath: Path): Boolean = {
    val parent: Path = outputPath.getParent
    val (name, _) = splitExtension(outputPath.toString)
    val matcher = FileSystems.getDefault().getPathMatcher(s"glob:$name*")
    Files.list(parent)
      .collect(Collectors.toList[Path])
      .asScala
      .exists(matcher.matches(_))
  }

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

    val parts = new ArrayList[Partition](List(partition).asJava)

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    val hdf5Loader = new Hdf5ImageLoader(hdf5Path.toFile, parts, null, false)

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
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
