package llsm

import cats.data.Xor
import java.nio.file.Path
import _root_.io.scif.ImageMetadata
// import _root_.io.scif.img.{ImgOpener}
// import _root_.io.scif.config.SCIFIOConfig

sealed trait LLSMIOError
case class MetadataIOError(message: String) extends LLSMIOError


package object io {

  def extractMetadata(pathList: List[Path]): Xor[LLSMIOError, ImageMetadata] = {
    val (metaPath :: tifList) = pathList.reverse
    if (!metaPath.endsWith(".txt"))
      return Xor.left(MetadataIOError("Error finding metadata file."))

    // Parse LLSM metadata from LLSM metadata txt file
    ???
  }

  /**
   * Parse LLSM metadata from filenames
   */
  def parseMetadataFromFileNames(imgFileList: List[Path]): Xor[LLSMIOError, ImageMetadata] = {
    ???
//    import cats.implicits._
//    val fnMeta: Option[List[FileNameMetadata]] = imgFileList.map(fn => MetadataParser[FileNameMetadata](fn.getFileName().toString)).sequence

//    fnMeta match {
//      case Some(FileNameMetadata(n, c, s, w, t, ta) :: tail) => {
//        val (channelIdxs, stackIdxs, wavelengths, times, timesAbs) = tail.foldLeft((Set[Int](c), Set[Int](s), Set[Int](w), Set[Long](t), Set[Long](ta))){
//        case ((c1, s1, w1, t1, ta1), FileNameMetadata(_, channel, stack, wavelength, time, timeA)) => (c1 + channel, s1 + stack, w1 + wavelength, t1 + time, ta1 + timeA)
//        }
//
//        Xor.right(LLSFileNameMeta(n, channelIdxs, stackIdxs, wavelengths, times, timesAbs))
//      }
//      case Some(Nil) => Xor.left(MetadataIOError("Error reading metadata from file names."))
//      case None => Xor.left(MetadataIOError("Error reading metadata from file names."))
//    }
  }
}
