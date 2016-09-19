package llsm
package io.metadata

import cats.implicits._
import scala.util.Try

trait ImplicitParsers {
  implicit val filenameParser = new Parser[FilenameMetadata] {
    def apply(s: String): Parser.Result[FilenameMetadata] = s.split("_").toList match {
      case List(name, channel, stack, cw, time, absTime) =>
        for {
          ch <- Either.fromTry(Try(channel.substring(2).toInt)).leftMap(e => ParsingFailure(s"Unable to parse channel number from file name: $s", e))
          s <- Either.fromTry(Try(stack.substring(5).toInt)).leftMap(e => ParsingFailure(s"Unable to parse stack index from file name: $s", e))
          w <- Either.fromTry(Try(cw.substring(0, cw.length-2).toInt)).leftMap(e => ParsingFailure(s"Unable to parse channel wavelength from file name: $s", e))
          t <- Either.fromTry(Try(time.substring(0, 7).toLong)).leftMap(e => ParsingFailure(s"Unable to parse timestamp from file name: $s", e))
          tAbs <- Either.fromTry(Try(absTime.substring(0, 10).toLong)).leftMap(e => ParsingFailure(s"Unable to parse absolute timestamp from file name: $s", e))
        } yield FilenameMetadata(name, ch, s, w, t, tAbs)
      case _ => Either.left(ParsingFailure(s"Malformed filename: $s", new Throwable("Filename does not match the expect string structure: name_channel#_stackIdx_channelWavelength_time_absTime.tif")))
    }
  }

  private[this] def channelFromLabel(s: String): Try[Int] = Try(s.substring(s.lastIndexOf("(") + 1, s.length - 3).trim.toInt)

  implicit val wfStageParser = new Parser[Waveform.Stage] {
    def apply(s: String): Parser.Result[Waveform.Stage] = s.split("\\t").toList match {
      case List(label, off, int, pix) => for {
        c <- Either.fromTry(channelFromLabel(label)).leftMap(e => ParsingFailure("Could not parse channel label", e))
        o <- Either.fromTry(Try(off.toDouble)).leftMap(e => ParsingFailure("Could not parse offset", e))
        i <- Either.fromTry(Try(int.toDouble)).leftMap(e => ParsingFailure("Could not parse interval", e))
        p <- Either.fromTry(Try(pix.toInt)).leftMap(e => ParsingFailure("Could not parse pixel number", e))
      } yield Waveform.Stage(c, o, i, p)
      case _ => Either.left(ParsingFailure("Could not parse stage metadata", new Throwable("Could not parse stage metadata")))
    }
  }

  implicit val wfStackParser = new Parser[Waveform.Stack] {
    def apply(s: String): Parser.Result[Waveform.Stack] = s.split("\\t").toList match {
      case List(label, sn) => for {
        c <- Either.fromTry(channelFromLabel(label)).leftMap(e => ParsingFailure("Could not parse channel label", e))
        s <- Either.fromTry(Try(sn.toInt)).leftMap(e => ParsingFailure("Could not parse stack number", e))
      } yield Waveform.Stack(c, s)
      case _ => Either.left(ParsingFailure("Could not parse the stack number from meta file", new Throwable(s"Could not parse the stack number from meta file:\n$s")))
    }
  }

  implicit val wfExcitationParser = new Parser[Waveform.Excitation] {
    def apply(s: String): Parser.Result[Waveform.Excitation] = s.split("\\t").toList match {
      case List(label, f, l, p, e) => for {
        c <- Either.fromTry(channelFromLabel(label)).leftMap(e => ParsingFailure("Could not parse channel label", e))
        filt <- Either.right(f)
        laser <- Either.fromTry(Try(l.toInt)).leftMap(err => ParsingFailure("Could not parse laser wavelength.", err))
        power <- Either.fromTry(Try(p.toInt)).leftMap(err => ParsingFailure("Could not parse laser power.", err))
        exp <- Either.fromTry(Try(e.toDouble)).leftMap(err => ParsingFailure("Could not parse laser exposure time.", err))
      } yield Waveform.Excitation(c, filt, laser, power, exp)
      case _ => Either.left(ParsingFailure("Unexpectd structure: could not parse laser settings from the metadat file.", new Throwable("Unexpectd structure: could not parse laser settings from the metadat file.")))
    }
  }

  implicit val wfTypeParser = new Parser[Waveform.Type] {
    def apply(s: String): Parser.Result[Waveform.Type] = s.split("\\t").toList match {
      case List(_, t) => Either.right(Waveform.Type(t))
      case _ => Either.left(ParsingFailure("Unexpected structure: unable to parse waveform type from metadata file.", new Throwable("Unexpected structure: unable to parse waveform type from metadata file.")))
    }
  }

  implicit val wfCycleParser = new Parser[Waveform.Cycle] {
    def apply(s: String): Parser.Result[Waveform.Cycle] = s.split("\\t").toList match {
      case List(_, cyc) => Either.right(Waveform.Cycle(cyc))
      case _ => Either.left(ParsingFailure("Unexpected structure: unable to parse cycle mode from metadata file.", new Throwable("Unexpected structure: unable to parse cycle mode from metadata file.")))
    }
  }

  implicit val wfZMotionParser = new Parser[Waveform.ZMotion] {
    def apply(s: String): Parser.Result[Waveform.ZMotion] = s.split("\\t").toList match {
      case List(_, zm) => Either.right(Waveform.ZMotion(zm))
      case _ => Either.left(ParsingFailure("Unexpected structure: unable to parse zmotion mode from metadata file.", new Throwable("Unexpected structure: unable to parse zmotion mode from metadata file.")))
    }
  }

  private[this] def sequenceChannels(xGalv: List[Waveform.Stage], zGalv: List[Waveform.Stage], zPZT: List[Waveform.Stage], sPZT: List[Waveform.Stage], stack: List[Waveform.Stack], excitation: List[Waveform.Excitation]): List[Waveform.Channel] = {
    for {
      xg <- xGalv
      zg <- zGalv
      zp <- zPZT
      sp <- sPZT
      s <- stack
      ex <- excitation
    } yield Waveform.Channel(xg.channel, s.number, xg, zg, zp, sp, ex)
  }

  implicit val wfParser = new Parser[Waveform] {
    def apply(s: String): Parser.Result[Waveform] = s.split("\\n\\n").toList match {
      case List(t, xg, zg, zp, sp, stacks, ex, cycle, zmotion) => for {
        wft <- Parser[Waveform.Type](t.trim)
        xGalv <- xg.split("\\n").toList.map(l => Parser[Waveform.Stage](l)).sequenceU
        zGalv <- zg.split("\\n").toList.map(l => Parser[Waveform.Stage](l)).sequenceU
        zPZT <- zp.split("\\n").toList.map(l => Parser[Waveform.Stage](l)).sequenceU
        sPZT <- sp.split("\\n").toList.map(l => Parser[Waveform.Stage](l)).sequenceU
        st <- stacks.split("\\n").toList.map(l => Parser[Waveform.Stack](l)).sequenceU
        exp <- ex.split("\\n").toList.map(l => Parser[Waveform.Excitation](l)).sequenceU
        cyc <- Parser[Waveform.Cycle](cycle.trim)
        zm <- Parser[Waveform.ZMotion](zmotion.trim)
      } yield Waveform(wft.name, sequenceChannels(xGalv, zGalv, zPZT, sPZT, st, exp), cyc.mode, zm.mode)
    }
  }

}

trait MetadataImplicits extends ImplicitParsers
