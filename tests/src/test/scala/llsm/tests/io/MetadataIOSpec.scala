package llsm.io

import java.nio.file.{Path, Paths}

import llsm.io.metadata.MetadataSuite
import llsm.io.metadata.FileMetadata
import org.scalatest.Matchers._

class MetadataIOSpec extends MetadataSuite {

  "readMetadataFromTxtFile" should "parse Waveform and Camera metadata from a text file" in {
    val f: Path = Paths.get(
      getClass
        .getResource("/io/data/Resolution test 4_Settings.txt")
        .toURI
        .getPath)

    val meta = FileMetadata.readMetadataFromTxtFile(f)

    meta should be('right)
  }
}
