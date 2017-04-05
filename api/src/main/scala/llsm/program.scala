package llsm

import java.nio.file.Path

import cats.free.Free
import cats.implicits._
import llsm.algebras.{ImgReader, ImgWriter, Metadata, Process, Progress}
import llsm.fp.ParSeq
import llsm.io.{LLSMImg, LLSMStack, ImgUtils}


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
  ): Free[F, LLSMStack] =
    for {
      m   <- Metadata[F].readMetadata(path)
      _   <- Progress[F].status(s"Reading: ${path.toString}")
      img <- ImgReader[F].readImg(path, m)
      _   <- Progress[F].status(s"Deskewing: ${path.toString}")
      deskewedImg <- Process[F].deskewImg(
        img,
        0,
        2,
        Deskew.calcShearFactor(m.waveform.sPZTInterval, m.sample.angle, m.config.xVoxelSize),
        m.config.interpolation)
    } yield deskewedImg

  def processImgs[F[_]: Metadata: ImgReader: Process: Progress](
      paths: List[Path]
  ): ParSeq[F, LLSMImg] =
    paths.traverse(p => ParSeq.liftSeq(processImg[F](p))).map(lImgs => ImgUtils.aggregateImgs(lImgs))

  def processStacks[F[_]: Metadata: ImgReader: Process: Progress](
      paths: List[Path]
  ): Free[F, LLSMImg] =
    for {
      ls  <- paths.traverse(p => processImg[F](p))
      img <- Process[F].aggregateImgs(ls)
    } yield img

  def convertStacks[F[_]: Metadata: ImgReader: Process: Progress: ImgWriter](
      paths: List[Path],
      outputPath: Path
  ): Free[F, Unit] =
    for {
      img <- processStacks[F](paths)
      r   <- ImgWriter[F].writeImg(outputPath, img)
    } yield r

  def convertImg[F[_]: Metadata: ImgReader: ImgWriter: Process: Progress](
      path: Path,
      outputPath: Path
  ): Free[F, Unit] =
    for {
      deskewedImg <- processImg[F](path)
      _ <- Progress[F].status(s"Writing: ${deskewedImg.meta.filename.name} ch: ${deskewedImg.meta.filename.channel} t: ${deskewedImg.meta.filename.stack}")
      r <- ImgWriter[F].writeImg(outputPath, deskewedImg)
    } yield r

  def convertImgs[F[_]: Metadata: ImgReader: Process: Progress: ImgWriter](
      paths: List[Path],
      outputPath: Path
  ): Free[F, List[Unit]] =
    paths.zipWithIndex.traverse {
      case (p, i) => for {
        r <- convertImg[F](p, outputPath)
        _ <- Progress[F].progress(i, paths.size)
      } yield r
    }

  def convertImgsP[F[_]: Metadata: ImgReader: Process: Progress: ImgWriter](
      paths: List[Path],
      outputPath: Path
  ): ParSeq[F, List[Unit]] =
    paths.zipWithIndex.traverse {
      case (p, i) => ParSeq.liftSeq(for {
        r <- convertImg[F](p, outputPath)
        _ <- Progress[F].progress(i, paths.size)
      } yield r)
    }
}
