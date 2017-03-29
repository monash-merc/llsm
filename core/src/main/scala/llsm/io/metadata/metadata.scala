package llsm.io.metadata

import java.lang.Math
import java.nio.file.{Files, Path}
import java.util.stream.Collectors

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Try, Success, Failure, Right, Left}

import io.scif.{ImageMetadata, SCIFIO}
import llsm.either._
import llsm.list._

sealed trait LLSMMeta
case class FileMetadata(
    filename: FilenameMetadata,
    waveform: WaveformMetadata,
    camera: CameraMetadata,
    sample: SampleStage,
    config: ConfigurableMetadata,
    image: Option[ImgMetadata] = None)
  extends LLSMMeta
case class AggregatedMeta(meta: ImageMetadata) extends LLSMMeta

object LLSMMeta extends MetadataImplicits with Ordering[LLSMMeta] {

  sealed abstract class MetadataError
  case class MetadataIOError(msg: String) extends MetadataError

  def compare(a: LLSMMeta, b: LLSMMeta): Int = (a, b) match {
    case (FileMetadata(FilenameMetadata(_, ac, _, _, at, _), _, _, _, _, _),
          FileMetadata(FilenameMetadata(_, bc, _, _, bt, _), _, _, _, _, _)) =>
      if (at == bt) ac.compare(bc)
      else at.compare(bt)
    case (am: AggregatedMeta, bm: AggregatedMeta) => 0
    case (am: FileMetadata, bm: AggregatedMeta) => -1
    case (am: AggregatedMeta, bm: FileMetadata) => 1

  }

  def extractMetadata(dir: Path, config: ConfigurableMetadata): Either[MetadataError, List[FileMetadata]] = {
    val files: List[Path] = Files.list(dir).collect(Collectors.toList[Path]).asScala.toList
    println(files)
    files
      .filter(f =>
        f.toString().endsWith(".txt") || f.toString().endsWith(".tif"))
      .partition(_.toString endsWith ".txt") match {
        case (List(), _) =>
          Left(
            MetadataIOError(
              s"Could not find metadata file in: ${dir.toString}"))
        case (_, List()) =>
          Left(
            MetadataIOError(s"No images were detected in: ${dir.toString}"))
        case (List(meta: Path), imgPaths) =>
          for {
            txtMeta <- readMetadataFromTxtFile(meta)
            fileMeta <- parseMetadataFromFileNames(imgPaths.toList)
            imgMeta <- extractImgMeta(imgPaths.toList, txtMeta)
          } yield imgMeta.zip(fileMeta).map {
            case (im, fm) => FileMetadata(fm, txtMeta.waveform, txtMeta.camera, txtMeta.sample, config, Some(im))
          }
      }
  }

  def readMetadataFromTxtFile(path: Path): Either[MetadataError, TextMetadata] = {
    val lines       = Source.fromFile(path.toString).getLines
    val wfDelimiter = "***** ***** ***** Waveform ***** ***** ***** "
    val camDelimiter =
      "\\*\\*\\*\\*\\* \\*\\*\\*\\*\\* \\*\\*\\*\\*\\*   Camera  \\*\\*\\*\\*\\* \\*\\*\\*\\*\\* \\*\\*\\*\\*\\* \\n"
    val timeDelimiter =
      "***** ***** *****   Advanced Timing  ***** ***** ***** "
    val wfLines: List[String] = lines
      .dropWhile(!_.equals(wfDelimiter))
      .takeWhile(!_.equals(timeDelimiter))
      .toList

    wfLines.mkString("\n").split(camDelimiter).toList match {
      case List(w, c) =>
        for {
          wf <- Parser[WaveformMetadata](w.trim).left.map {
            case ParsingFailure(m, e) => MetadataIOError(s"$m:\n$e")
          }
          cam <- Parser[CameraMetadata](c.trim).left.map {
            case ParsingFailure(m, e) => MetadataIOError(s"$m:\n$e")
          }
          sample <- Parser[SampleStage](lines.filter(_.startsWith("Angle between stage and bessel beam")).next).left.map {
            case ParsingFailure(m, e) => MetadataIOError(s"$m:\n$e")
          }
        } yield TextMetadata(wf, cam, sample)
    }
  }

  /**
    * Parse LLSM metadata from filenames
    */
  def parseMetadataFromFileNames(imgFileList: List[Path]): Either[MetadataError, List[FilenameMetadata]] = {
    val fnMeta: Parser.Result[List[FilenameMetadata]] =
      sequenceListEither(imgFileList.map(path => Parser[FilenameMetadata](path.getFileName().toString)))

    fnMeta.left.map { case ParsingFailure(m, e) => MetadataIOError(s"$m:\n$e") }
  }

  def extractImgMeta(imgFileList: List[Path], textMeta: TextMetadata): Either[MetadataError, List[ImgMetadata]] = {
    val scifio: SCIFIO = new SCIFIO()
    sequenceListEither(imgFileList.map(
      p => Try {
        val format = scifio.format().getFormat(p.toString)
        format.createParser().parse(p.toString)
      } match {
        case Success(meta) => {
          val im = meta.get(0)
          val zInterval = Math.sin(Math.toRadians(textMeta.sample.angle)) * textMeta.waveform.sPZTInterval + textMeta.waveform.zPZTInterval
          val cals = Array[Double](0.1018, 0.1018, zInterval)
          Right(ImgMetadata(im.getAxesLengths, cals))
        }
        case Failure(e) => Left(MetadataIOError(e.getMessage))
      }))
    }
}
