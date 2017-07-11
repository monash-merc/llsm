package llsm

import java.nio.file.Path

import cats.free.Free
import cats.implicits._
import llsm.algebras.{
  ImgReader,
  ImgWriter,
  Metadata,
  Process, 
  Progress
}
import llsm.fp.ParSeq
import llsm.io.LLSMImg


/**
  * Tools for reading, parsing and writing LLSM images and metadata
  * Starting point is typically a directory containing LLSM image stacks
  * and a raw metadata file.
  *  img_dir __ readFileNameMeta __ ImageMetadata
  *          \_ readWaveformMeta _/
  */
object Programs {

  def processImg[F[_]: Metadata: ImgReader: Process: Progress](
      path: Path
  ): Free[F, LLSMImg] =
    for {
      _   <- Progress[F].status(s"Reading metadata: ${path.toString}")
      m   <- Metadata[F].readMetadata(path)
      _   <- Progress[F].status(s"Reading: ${path.toString}")
      img <- ImgReader[F].readImg(path, m)
      _   <- Progress[F].status(s"Deskewing: ${path.toString}")
      deskewedImg <- Process[F].deskewImg(
        img,
        0,
        2,
        Deskew.calcShearFactor(
          m.waveform.sPZTInterval,
          m.sample.angle,
          m.config.xVoxelSize),
        m.config.interpolation)
    } yield deskewedImg


  def convertImg[F[_]: Metadata: ImgReader: ImgWriter: Process: Progress](
      path: Path,
      outputPath: Path
  ): Free[F, LLSMImg] =
    for {
      deskewedImg <- processImg[F](path)
      _ <- Progress[F].status(s"Writing: ${deskewedImg.meta.filename.name} ch: ${deskewedImg.meta.filename.channel} t: ${deskewedImg.meta.filename.stack}")
      m <- ImgWriter[F].writeImg(outputPath, deskewedImg)
    } yield deskewedImg.copy(meta = m)

  def convertImgs[F[_]: Metadata: ImgReader: Process: Progress: ImgWriter](
      paths: List[Path],
      outputPath: Path
  ): Free[F, List[LLSMImg]] =
    paths.zipWithIndex.traverse {
      case (p, i) => for {
        m <- convertImg[F](p, outputPath)
        _ <- Progress[F].progress(i, paths.size)
      } yield m
    }

  def convertImgsP[F[_]: Metadata: ImgReader: Process: Progress: ImgWriter](
      paths: List[Path],
      outputPath: Path
  ): ParSeq[F, List[LLSMImg]] = {
    paths.zipWithIndex.traverse {
      case (p, i) =>
        ParSeq.liftSeq(for {
          m <- convertImg[F](p, outputPath)
          _ <- Progress[F].progress(i, paths.size)
        } yield m)
    }
  }
}
