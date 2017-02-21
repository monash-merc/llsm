package llsm.io
package metadata

import java.io.File

import cats.implicits._
import scala.io.Source

case class Metadata(filename: List[FilenameMetadata], waveform: WaveformMetadata, camera: CameraMetadata)

object Metadata extends MetadataImplicits {

  sealed abstract class MetadataError
  case class MetadataIOError(msg: String) extends MetadataError

  def extractMetadata(dir: File): Either[MetadataError, Metadata] =
    dir.listFiles.toList match {
      case null => Either.left(MetadataIOError(s"$dir is not a directory!"))
      case Nil  => Either.left(MetadataIOError(s"$dir is empty."))
      case files =>
        files
          .filter(f =>
            f.getName().endsWith(".txt") || f.getName().endsWith(".tif"))
          .partition(_.getName endsWith ".txt") match {
          case (List(), _) =>
            Either.left(
              MetadataIOError(
                s"Could not find metadata file in: ${dir.getPath}"))
          case (_, List()) =>
            Either.left(
              MetadataIOError(s"No images were detected in: ${dir.getPath}"))
          case (List(meta), imgPaths) =>
            for {
              txtMeta <- readMetadataFromTxtFile(meta)
              fnMeta <- parseMetadataFromFileNames(
                imgPaths.map(p => p.getName))
            } yield Metadata(fnMeta, txtMeta.waveform, txtMeta.camera)
        }
    }

  def readMetadataFromTxtFile(path: File): Either[MetadataError, TextMetadata] = {
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
          wf <- Parser[WaveformMetadata](w.trim).leftMap {
            case ParsingFailure(m, e) => MetadataIOError(s"$m:\n$e")
          }
          cam <- Parser[CameraMetadata](c.trim).leftMap {
            case ParsingFailure(m, e) => MetadataIOError(s"$m:\n$e")
          }
        } yield TextMetadata(wf, cam)
    }
  }

  /**
    * Parse LLSM metadata from filenames
    */
  def parseMetadataFromFileNames(imgFileList: List[String])
    : Either[MetadataError, List[FilenameMetadata]] = {
    val fnMeta: Parser.Result[List[FilenameMetadata]] =
      imgFileList.map(fn => Parser[FilenameMetadata](fn)).sequenceU

    fnMeta.leftMap { case ParsingFailure(m, e) => MetadataIOError(s"$m:\n$e") }
  }

}
