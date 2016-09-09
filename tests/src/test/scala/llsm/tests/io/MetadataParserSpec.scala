package llsm.io.metadata

import org.scalatest.Matchers._
import org.scalacheck.Gen
import cats.data.Xor

class MetadataParserSpec extends MetadataSuite {

  "A Parser[String]" should " simply return the string" in {
    val in: String = "Hellow"

    assert(Parser[String](in).getOrElse("Ouch") == "Hellow")
  }
  it should "parse all strings" in forAll { (s: String) =>
    assert(Parser[String](s) == Xor.right(s))
  }

  "A Parser[Int]" should "parse an int from a string" in {
    val in: String = "1"

    assertResult(1)(Parser[Int](in).getOrElse("Ouch"))
  }
  it should "fail with an Error when the string cannont be cast to an integer" in {
    val hello: String = "hello"

    Parser[Int](hello) should be ('left)
  }

  "A Parser[Waveform]" should "parse LLSM waveform metadata from a block of text" in {
    val wave = """Waveform type :	Linear

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
    val wf = Parser[Waveform](wave)

    wf match {
      case Xor.Right(Waveform(wt, xGalv, zGalv, zPZT, sPZT, st, ex, cyc, zm)) => {
        assertResult(Waveform.ZMotion("Sample piezo"))(zm)
        assertResult(Waveform.Cycle("per Z"))(cyc)
        assertResult(List(Waveform.Excitation(0, "N/A", 561, 2, 3), Waveform.Excitation(1, "N/A", 488, 20, 3)))(ex)
        assertResult(List(Waveform.Stage(0, 0, 0.1, 101), Waveform.Stage(1, 0, 0.1, 101)))(xGalv)
      }
      case Xor.Left(ParsingFailure(m, e)) => fail(s"$m:\n$e")
    }
  }

  "A Parser[FilenameMetadata]" should "parse LLSM Metadata from a list of LLSM data files" in {
    val file: String = "Resolution test 4_ch1_stack0001_488nm_0007480msec_0008884606msecAbs.tif"

    Parser[FilenameMetadata](file) match {
      case Xor.Right(FilenameMetadata(n, c, tidx, wl, ts, ats)) => {
        assertResult("Resolution test 4")(n)
        assertResult(1)(c)
        assertResult(1)(tidx)
        assertResult(488)(wl)
        assertResult(7480L)(ts)
        assertResult(8884606L)(ats)
      }
      case Xor.Left(ParsingFailure(m, e)) => fail(s"$m:\n$e")
    }
  }
  it should "parse any channel and stack number >= 0" in forAll(Gen.choose(0, Integer.MAX_VALUE), Gen.choose(0, 9999)) { (c: Int, s: Int) =>
    whenever(c >= 0 && s >= 0 && s <= 9999) {
      val paddedS = "%04d".format(s)

      val fn: String = s"Resolution test 4_ch${c}_stack${paddedS}_488nm_0007480msec_0008884606msecAbs.tif"
      val pfn: Parser.Result[FilenameMetadata] = Parser[FilenameMetadata](fn)
      pfn should be ('right)
      pfn should be (Xor.right(FilenameMetadata("Resolution test 4", c, s, 488, 7480L, 8884606L)))
    }
  }
  it should "parse any wavelength 0 <= wl <= 9999" in forAll(Gen.choose(0, 9999)) { (w: Int) =>
    whenever(w >= 0 && w <= 9999) {
      val fn: String = s"Resolution test 4_ch1_stack0001_${w}nm_0007480msec_0008884606msecAbs.tif"

      val pfn: Parser.Result[FilenameMetadata] = Parser[FilenameMetadata](fn)

      pfn should be ('right)
      pfn should be (Xor.right(FilenameMetadata("Resolution test 4", 1, 1, w, 7480L, 8884606L)))
    }
  }
  it should "fail when passed a malformed file name" in {
    val file: String = "Test_hello.txt"

    Parser[FilenameMetadata](file) should be ('left)
  }
  it should "fail when the channel number isn't an Int" in {
    val file: String = "Resolution test 4_chh_stack0001_488nm_0007480msec_0008884606msecAbs.tif"

    Parser[FilenameMetadata](file) should be ('left)
  }
  a [NumberFormatException] should be thrownBy {
    val file: String = "Resolution test 4_ch1_stack000b_488nm_0007480msec_0008884606msecAbs.tif"

    Parser[FilenameMetadata](file) match {
      case Xor.Left(ParsingFailure(_, e)) => throw e
      case _ => fail("Expected a Xor.Left with ParsingFailure but got Xor.Right")
    }
  }
}
