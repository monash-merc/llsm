package llsm
package io.metadata

import cats.implicits._
import scala.util.Try

trait ImplicitParsers {
  implicit val filenameParser = new Parser[FilenameMetadata] {
    def apply(s: String): Parser.Result[FilenameMetadata] =
      s.split("_").toList match {
        case List(name, channel, stack, cw, time, absTime) =>
          for {
            ch <- Either
              .fromTry(Try(channel.substring(2).toInt))
              .leftMap(
                e =>
                  ParsingFailure(
                    s"Unable to parse channel number from file name: $s",
                    e))
            s <- Either
              .fromTry(Try(stack.substring(5).toInt))
              .leftMap(
                e =>
                  ParsingFailure(
                    s"Unable to parse stack index from file name: $s",
                    e))
            w <- Either
              .fromTry(Try(cw.substring(0, cw.length - 2).toInt))
              .leftMap(
                e =>
                  ParsingFailure(
                    s"Unable to parse channel wavelength from file name: $s",
                    e))
            t <- Either
              .fromTry(Try(time.substring(0, 7).toLong))
              .leftMap(e =>
                ParsingFailure(s"Unable to parse timestamp from file name: $s",
                               e))
            tAbs <- Either
              .fromTry(Try(absTime.substring(0, 10).toLong))
              .leftMap(
                e =>
                  ParsingFailure(
                    s"Unable to parse absolute timestamp from file name: $s",
                    e))
          } yield FilenameMetadata(name, ch, s, w, t, tAbs)
        case _ =>
          Either.left(
            ParsingFailure(
              s"Malformed filename: $s",
              new Throwable(
                "Filename does not match the expect string structure: name_channel#_stackIdx_channelWavelength_time_absTime.tif")
            ))
      }
  }

  private[this] def channelFromLabel(s: String): Try[Int] =
    Try(s.substring(s.lastIndexOf("(") + 1, s.length - 3).trim.toInt)

  implicit val wfStageParser = new Parser[WaveformMetadata.Stage] {
    def apply(s: String): Parser.Result[WaveformMetadata.Stage] =
      s.split("\\t").toList match {
        case List(label, off, int, pix) =>
          for {
            c <- Either
              .fromTry(channelFromLabel(label))
              .leftMap(e => ParsingFailure("Could not parse channel label", e))
            o <- Either
              .fromTry(Try(off.toDouble))
              .leftMap(e => ParsingFailure("Could not parse offset", e))
            i <- Either
              .fromTry(Try(int.toDouble))
              .leftMap(e => ParsingFailure("Could not parse interval", e))
            p <- Either
              .fromTry(Try(pix.toLong))
              .leftMap(e => ParsingFailure("Could not parse pixel number", e))
          } yield WaveformMetadata.Stage(c, o, i, p)
        case _ =>
          Either.left(
            ParsingFailure("Could not parse stage metadata",
                           new Throwable("Could not parse stage metadata")))
      }
  }

  implicit val wfStackParser = new Parser[WaveformMetadata.Stack] {
    def apply(s: String): Parser.Result[WaveformMetadata.Stack] =
      s.split("\\t").toList match {
        case List(label, sn) =>
          for {
            c <- Either
              .fromTry(channelFromLabel(label))
              .leftMap(e => ParsingFailure("Could not parse channel label", e))
            s <- Either
              .fromTry(Try(sn.toInt))
              .leftMap(e => ParsingFailure("Could not parse stack number", e))
          } yield WaveformMetadata.Stack(c, s)
        case _ =>
          Either.left(
            ParsingFailure(
              "Could not parse the stack number from meta file",
              new Throwable(
                s"Could not parse the stack number from meta file:\n$s")))
      }
  }

  implicit val wfExcitationParser = new Parser[WaveformMetadata.Excitation] {
    def apply(s: String): Parser.Result[WaveformMetadata.Excitation] =
      s.split("\\t").toList match {
        case List(label, f, l, p, e) =>
          for {
            c <- Either
              .fromTry(channelFromLabel(label))
              .leftMap(e => ParsingFailure("Could not parse channel label", e))
            filt <- Either.right(f)
            laser <- Either
              .fromTry(Try(l.toInt))
              .leftMap(err =>
                ParsingFailure("Could not parse laser wavelength.", err))
            power <- Either
              .fromTry(Try(p.toInt))
              .leftMap(err =>
                ParsingFailure("Could not parse laser power.", err))
            exp <- Either
              .fromTry(Try(e.toDouble))
              .leftMap(err =>
                ParsingFailure("Could not parse laser exposure time.", err))
          } yield WaveformMetadata.Excitation(c, filt, laser, power, exp)
        case _ =>
          Either.left(
            ParsingFailure(
              "Unexpectd structure: could not parse laser settings from the metadat file.",
              new Throwable(
                "Unexpectd structure: could not parse laser settings from the metadat file.")
            ))
      }
  }

  implicit val wfTypeParser = new Parser[WaveformMetadata.Type] {
    def apply(s: String): Parser.Result[WaveformMetadata.Type] =
      s.split("\\t").toList match {
        case List(_, t) => Either.right(WaveformMetadata.Type(t))
        case _ =>
          Either.left(
            ParsingFailure(
              "Unexpected structure: unable to parse waveform type from metadata file.",
              new Throwable(
                "Unexpected structure: unable to parse waveform type from metadata file.")
            ))
      }
  }

  implicit val wfCycleParser = new Parser[WaveformMetadata.Cycle] {
    def apply(s: String): Parser.Result[WaveformMetadata.Cycle] =
      s.split("\\t").toList match {
        case List(_, cyc) => Either.right(WaveformMetadata.Cycle(cyc))
        case _ =>
          Either.left(
            ParsingFailure(
              "Unexpected structure: unable to parse cycle mode from metadata file.",
              new Throwable(
                "Unexpected structure: unable to parse cycle mode from metadata file.")
            ))
      }
  }

  implicit val wfZMotionParser = new Parser[WaveformMetadata.ZMotion] {
    def apply(s: String): Parser.Result[WaveformMetadata.ZMotion] =
      s.split("\\t").toList match {
        case List(_, zm) => Either.right(WaveformMetadata.ZMotion(zm))
        case _ =>
          Either.left(
            ParsingFailure(
              "Unexpected structure: unable to parse zmotion mode from metadata file.",
              new Throwable(
                "Unexpected structure: unable to parse zmotion mode from metadata file.")
            ))
      }
  }

  private[this] def sequenceChannels(
      xGalv: List[WaveformMetadata.Stage],
      zGalv: List[WaveformMetadata.Stage],
      zPZT: List[WaveformMetadata.Stage],
      sPZT: List[WaveformMetadata.Stage],
      stack: List[WaveformMetadata.Stack],
      excitation: List[WaveformMetadata.Excitation])
    : List[WaveformMetadata.Channel] = {
    for {
      xg <- xGalv
      zg <- zGalv
      zp <- zPZT
      sp <- sPZT
      s  <- stack
      ex <- excitation
    } yield WaveformMetadata.Channel(xg.channel, s.number, xg, zg, zp, sp, ex)
  }

  implicit val wfParser = new Parser[WaveformMetadata] {
    def apply(s: String): Parser.Result[WaveformMetadata] =
      s.split("\\n\\n").toList match {
        case List(t, xg, zg, zp, sp, stacks, ex, cycle, zmotion) =>
          for {
            wft <- Parser[WaveformMetadata.Type](t.trim)
            xGalv <- xg
              .split("\\n")
              .toList
              .map(l => Parser[WaveformMetadata.Stage](l))
              .sequenceU
            zGalv <- zg
              .split("\\n")
              .toList
              .map(l => Parser[WaveformMetadata.Stage](l))
              .sequenceU
            zPZT <- zp
              .split("\\n")
              .toList
              .map(l => Parser[WaveformMetadata.Stage](l))
              .sequenceU
            sPZT <- sp
              .split("\\n")
              .toList
              .map(l => Parser[WaveformMetadata.Stage](l))
              .sequenceU
            st <- stacks
              .split("\\n")
              .toList
              .map(l => Parser[WaveformMetadata.Stack](l))
              .sequenceU
            exp <- ex
              .split("\\n")
              .toList
              .map(l => Parser[WaveformMetadata.Excitation](l))
              .sequenceU
            cyc <- Parser[WaveformMetadata.Cycle](cycle.trim)
            zm  <- Parser[WaveformMetadata.ZMotion](zmotion.trim)
          } yield {
            WaveformMetadata(
              wft.name,
              sPZT(0).excitationPixels,
              zPZT(0).interval,
              sPZT(0).interval,
              sequenceChannels(xGalv, zGalv, zPZT, sPZT, st, exp),
              st(0).number,
              cyc.mode,
              zm.mode)
          }
      }
  }

  implicit val cameraRoiParser = new Parser[CameraMetadata.ROI] {
    def apply(s: String): Parser.Result[CameraMetadata.ROI] =
      s.split(" ").toList match {
        case List(l, t, r, b) =>
          for {
            left   <- Parser[Long](l.split("=")(1))
            top    <- Parser[Long](t.split("=")(1))
            right  <- Parser[Long](r.split("=")(1))
            bottom <- Parser[Long](b.split("=")(1))
          } yield CameraMetadata.ROI(left, top, right, bottom)
        case _ => {
          val mess: String = "Unable to parse Camera ROI meta"
          Either.left(ParsingFailure(mess, new Throwable(mess)))
        }
      }
  }

  implicit val cameraCoordParser = new Parser[CameraMetadata.Coord] {
    def apply(s: String): Parser.Result[CameraMetadata.Coord] =
      s.split(" ").toList match {
        case List(xs, ys) =>
          for {
            x <- Parser[Long](xs.split("=")(1))
            y <- Parser[Long](xs.split("=")(1))
          } yield CameraMetadata.Coord(x, y)
        case _ => {
          val mess: String = "Unable to parse Camera Coord meta"
          Either.left(ParsingFailure(mess, new Throwable(mess)))
        }
      }
  }

  implicit val cameraParser = new Parser[CameraMetadata] {
    def apply(s: String): Parser.Result[CameraMetadata] =
      s.split("\\n").toList match {
        case l: List[String] if l.size == 31 =>
          for {
            model         <- Parser[String](l(0).trim.split("\\t")(1))
            serial        <- Parser[Int](l(1).trim.split("\\t")(1))
            frameTransfer <- Parser[String](l(2).trim.split("\\t")(1))
            trig          <- Parser[String](l(3).trim.split("\\t")(1))
            exposure      <- Parser[Double](l(4).trim.split("\\t")(1))
            cycleT        <- Parser[Double](l(5).trim.split("\\t")(1))
            cycleR        <- Parser[Double](l(6).trim.split("\\t")(1).split(" ")(0))
            frameMode     <- Parser[String](l(7).trim.split("\\t")(1))
            readMode      <- Parser[String](l(8).trim.split("\\t")(1))
            roi           <- Parser[CameraMetadata.ROI](l(9).trim.split("\\t")(1))
            binning       <- Parser[CameraMetadata.Coord](l(10).trim.split("\\t")(1))
            pixels        <- Parser[CameraMetadata.Coord](l(11).trim.split("\\t")(1))
            vssSpeed      <- Parser[Double](l(12).trim.split("\\t")(1))
            hssSpeed      <- Parser[Double](l(13).trim.split("\\t")(1))
            vssAmp        <- Parser[String](l(14).trim.split("\\t")(1))
            outAmp        <- Parser[String](l(15).trim.split("\\t")(1))
            temp          <- Parser[Double](l(16).trim.split("\\t")(1))
            adChannel     <- Parser[Int](l(17).trim.split("\\t")(1))
            emGain        <- Parser[Long](l(18).trim.split("\\t")(1))
            hsSpeedIndex  <- Parser[Int](l(19).trim.split("\\t")(1))
            preampGain    <- Parser[Long](l(20).trim.split("\\t")(1))
            bitDepth      <- Parser[Int](l(21).trim.split("\\t")(1))
            baslineClamp  <- Parser[String](l(22).trim.split("\\t")(1))
            spool         <- Parser[String](l(23).trim.split("\\t")(1))
            expNumber     <- Parser[Long](l(24).trim.split("\\t")(1).split(" ")(0))
            cropped       <- Parser[String](l(25).trim.split("\\t")(1))
            lRoi          <- Parser[CameraMetadata.ROI](l(26).trim.split("\\t")(1))
            rRoi          <- Parser[CameraMetadata.ROI](l(27).trim.split("\\t")(1))
            fovRoi        <- Parser[CameraMetadata.ROI](l(28).trim.split("\\t")(1))
            imgNumber     <- Parser[Long](l(29).trim.split("\\t")(1).split(" ")(0))
            subRois       <- Parser[String](l(30).trim.split("\\t")(1))
          } yield
            CameraMetadata(
              model,
              serial,
              frameTransfer,
              trig,
              exposure,
              cycleT,
              cycleR,
              frameMode,
              readMode,
              roi,
              binning,
              pixels,
              vssSpeed,
              hssSpeed,
              vssAmp,
              outAmp,
              temp,
              adChannel,
              emGain,
              hsSpeedIndex,
              preampGain,
              bitDepth,
              baslineClamp,
              spool,
              expNumber,
              cropped,
              lRoi,
              rRoi,
              fovRoi,
              imgNumber,
              subRois
            )
        case _ => {
          val mess: String = "Unable to parse Camera meta"
          Either.left(ParsingFailure(mess, new Throwable(mess)))
        }
      }
  }
}

trait MetadataImplicits extends ImplicitParsers
