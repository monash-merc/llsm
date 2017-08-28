package llsm.io.metadata

import java.util.UUID

import org.scalatest.Matchers._
import org.scalacheck.Gen

class MetadataParserSpec extends MetadataSuite {

  "A Parser[String]" should " simply return the string" in {
    val in: String = "Hellow"

    assert(Parser[String](in) == Either.right("Hellow"))
  }
  it should "parse all strings" in forAll { (s: String) =>
    assert(Parser[String](s) == Either.right(s))
  }

  "A Parser[Int]" should "parse an int from a string" in {
    val in: String = "1"

    assertResult(Right(1))(Parser[Int](in))
  }
  it should "fail with an Error when the string cannont be cast to an integer" in {
    val hello: String = "hello"

    Parser[Int](hello) should be('left)
  }

  "A Parser[Waveform]" should "parse LLSM waveform metadata from a block of text" in {
    val wave =
      """Waveform type :	Linear

X Galvo Offset, Interval (um), # of Pixels for Excitation (0) :	0	0.1	101
X Galvo Offset, Interval (um), # of Pixels for Excitation (1) :	0	0.1	101

Z Galvo Offset, Interval (um), # of Pixels for Excitation (0) :	0.45	0	251
Z Galvo Offset, Interval (um), # of Pixels for Excitation (1) :	0.45	0	251

Z PZT Offset, Interval (um), # of Pixels for Excitation (0) :	12	0	251
Z PZT Offset, Interval (um), # of Pixels for Excitation (1) :	12	0	251

S PZT Offset, Interval (um), # of Pixels for Excitation (0) :	50	0.3	251
S PZT Offset, Interval (um), # of Pixels for Excitation (1) :	50	0.3	251

# of stacks (0) :	20
# of stacks (1) :	20

Excitation Filter, Laser, Power (%), Exp(ms) (0) :	N/A	561	2	3
Excitation Filter, Laser, Power (%), Exp(ms) (1) :	N/A	488	20	3

Cycle lasers :	per Z

Z motion :	Sample piezo"""
    val wf = Parser[WaveformMetadata](wave)

    wf match {
      case Right(
          WaveformMetadata(wt,
                           nSlices,
                           zPZTOffset,
                           sPZTOffset,
                           channels,
                           nFrames,
                           cyc,
                           zm)) => {
        assertResult("Linear")(wt)
        assertResult("Sample piezo")(zm)
        assertResult("per Z")(cyc)
        assertResult(251)(nSlices)
        assertResult(0)(zPZTOffset)
        assertResult(0.3)(sPZTOffset)
        assertResult(20)(nFrames)
        assertResult(2)(channels.size)
        assertResult(0)(channels(0).id)
      }
      case Left(ParsingFailure(m, e)) => fail(s"$m:\n$e")
    }
  }

  "A Parser[CameraMetadata]" should "parse CameraMetadata from a block of text" in {
    val text: String =
      """Model :	C11440-22C
Serial :	3296
Frame Transfer :	OFF
Trigger :	External
Exp(s) :	0.00101
Cycle(s) :	0.00226
Cycle(Hz) :	442.34 Hz
Frame Mode :	Run Till Abort
Readout Mode :	Image
ROI :	Left=897 Top=897 Right=1152 Bot=1152
Binning :	X=1 Y=1
# of Pixels :	X=2048 Y=2048
VSSspeed :	-1.0000
HSSspeed :	0.0000
VSSAmplitude :	Normal
Output Amplifier :	EM Amp
Temp(C) :	0.00000
AD Channel :	0
EM Gain :	0
HS Speed index :	0
Preamp Gain :	0
Bit depth :	0
Baseline Clamp :	Yes
Spool :	Enabled?=No File stem=Unknown type # buffs=10 Method=16-buit TIFF
# of Exps :	10040 exp(s)
Cropped :	On?=No # of pix=X=2048 Y=2048  Bin=X=1 Y=1
L ROI :	Left=897 Top=897 Right=0 Bot=1152
R ROI :	Left=0 Top=897 Right=1152 Bot=1152
FOV ROI :	Left=897 Top=897 Right=1152 Bot=1152
# of Imgs :	10040 img(s)
subROIs :	Unknown type
"""

    val cam = Parser[CameraMetadata](text)

    cam match {
      case Right(c: CameraMetadata) => {
        assertResult("C11440-22C")(c.model)
        assertResult(CameraMetadata.ROI(897L, 897L, 1152L, 1152L))(c.roi)
        assertResult(10040)(c.imgNumber)
        assertResult(0)(c.bitDepth)
      }
      case Left(ParsingFailure(m, e)) => fail(s"$m:\n$e")
    }

  }

  "A Parser[SampleStage]" should "parse the angle between the stage and bessel as a Double" in forAll("ang") {
    (ang: Double) =>
      val str: String = s"Angle between stage and bessel beam (deg) = $ang"

      Parser[SampleStage](str) match {
        case Right(SampleStage(a)) => a should equal (ang)
        case Left(ParsingFailure(m, e)) => fail(s"$m:\n$e")
      }
  }

  "A Parser[FilenameMetadata]" should "parse LLSM Metadata from a list of LLSM data files" in {
    val file: String =
      "Resolution test 4_ch1_stack0001_488nm_0007480msec_0008884606msecAbs.tif"

    Parser[FilenameMetadata](file) match {
      case Right(FilenameMetadata(id, n, c, tidx, wl, ts, ats)) => {
        assertResult(UUID.nameUUIDFromBytes(file.getBytes))(id)
        assertResult("Resolution test 4")(n)
        assertResult(1)(c)
        assertResult(1)(tidx)
        assertResult(488)(wl)
        assertResult(7480L)(ts)
        assertResult(8884606L)(ats)
      }
      case Left(ParsingFailure(m, e)) => fail(s"$m:\n$e")
    }
  }
  it should "parse any channel and stack number >= 0" in forAll(
    Gen.choose(0, Integer.MAX_VALUE),
    Gen.choose(0, 9999)) { (c: Int, s: Int) =>
    whenever(c >= 0 && s >= 0 && s <= 9999) {
      val paddedS = "%04d".format(s)

      val fn: String =
        s"Resolution test 4_ch${c}_stack${paddedS}_488nm_0007480msec_0008884606msecAbs.tif"
      val pfn: Parser.Result[FilenameMetadata] = Parser[FilenameMetadata](fn)
      pfn should be('right)
      pfn should be(
        Either.right(
          FilenameMetadata(UUID.nameUUIDFromBytes(fn.getBytes), "Resolution test 4", c, s, 488, 7480L, 8884606L)))
    }
  }
  it should "parse any wavelength 0 <= wl <= 9999" in forAll(
    Gen.choose(0, 9999)) { (w: Int) =>
    whenever(w >= 0 && w <= 9999) {
      val fn: String =
        s"Resolution test 4_ch1_stack0001_${w}nm_0007480msec_0008884606msecAbs.tif"

      val pfn: Parser.Result[FilenameMetadata] = Parser[FilenameMetadata](fn)

      pfn should be('right)
      pfn should be(
        Either.right(
          FilenameMetadata(UUID.nameUUIDFromBytes(fn.getBytes), "Resolution test 4", 1, 1, w, 7480L, 8884606L)))
    }
  }
  it should "successly parse when the name has underscores in it" in {
    val fn: String =
      s"Resolution_test_4_ch1_stack0001_488nm_0007480msec_0008884606msecAbs.tif"

    val pfn: Parser.Result[FilenameMetadata] = Parser[FilenameMetadata](fn)

    pfn should be('right)
    pfn should be(
      Either.right(
        FilenameMetadata(UUID.nameUUIDFromBytes(fn.getBytes), "Resolution_test_4", 1, 1, 488, 7480L, 8884606L)))
  }
  it should "fail when passed a malformed file name" in {
    val file: String = "Test_hello.txt"

    Parser[FilenameMetadata](file) should be('left)
  }
  it should "fail when the channel number isn't an Int" in {
    val file: String =
      "Resolution test 4_chh_stack0001_488nm_0007480msec_0008884606msecAbs.tif"

    Parser[FilenameMetadata](file) should be('left)
  }
  a[NumberFormatException] should be thrownBy {
    val file: String =
      "Resolution test 4_ch1_stack000b_488nm_0007480msec_0008884606msecAbs.tif"

    Parser[FilenameMetadata](file) match {
      case Left(ParsingFailure(_, e)) => throw e
      case _ =>
        fail("Expected a Either.Left with ParsingFailure but got Either.Right")
    }
  }
}
