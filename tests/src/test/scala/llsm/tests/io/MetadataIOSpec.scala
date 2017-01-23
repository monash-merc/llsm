package llsm.io

import llsm.io.metadata.MetadataSuite
import java.io.File
import org.scalatest.Matchers._

class MetadataIOSpec extends MetadataSuite {

  "readMetadataFromTxtFile" should "parse Waveform and Camera metadata from a text file" in {
    val f: File = new File(
      getClass
        .getResource("/io/data/Resolution test 4_Settings.txt")
        .toURI
        .getPath)

    val meta = readMetadataFromTxtFile(f)

    meta should be('right)
  }
}
