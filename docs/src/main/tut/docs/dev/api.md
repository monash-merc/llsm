---
layout: docs
title: API Module
section: develop
---

## llsm api
This is level at which most developers will want to interact with the `llsm` libraries. This module provides a Free monad based DSL for processing LLSM data. You can find more general information about Free monads in Scala at the excellent [Typelevel Cats documentation site](https://typelevel.org/cats/). More relevant to the actual implementation used here is a series of blog posts by John Degoes ([1](http://degoes.net/articles/modern-fp), [2](http://degoes.net/articles/modern-fp-part-2)) that describe a way to provide modular DSLs and interpreters that can be composed both vertically and horizontally.

Free monads may be a little daunting when you first jump in, but one doesn't need to understand them intimately to become productive with the DSL. That said, one important concept to grasp is that the declaration of the program or pipeline is separated from it's execution. Programs are defined either directly through the DSL or by composes a series of smaller programs into a larger one. This program is declarative way of describing the flow of data, but doesn't actually provide any actual implementation of the processing of the data. Actual execution/processing of the programs is defined separately through an interpreter that interpets the DSL and does the actual hard work. It is strongly recommended that you following the tutorials on the Cats site to gain an insight into the separation between declaration of the program and its execution.

Interpreters too can be composed such that they can fully execute a program and/or provide auxillary functionality (e.g., logging or progress reporting). This is an important insight that is further explained in the blog posts by John Degoes (see above).

The DSLs/algebras provided by the `api` module are as follows:

- Metadata
- ImgReader
- ImgWriter
- Process
- Progress
- Visualise

While the details of how the algebras are defined is not that important (it's actually somewhat cumbersome in Scala), you can find the implementations in the `llsm.algerbras` package. The api modules also provide some basic inteprepters that can execute the DSLs above. Simple implementations can be found in the `llsm.interpreters` package; however, .

`llsm` strives for purity and as a result all intepreters are parameterised with an effect type, which allows effecting work to be executed in a constrained environment that captures errors etc.

## Example
Let's dive in and see some code. The following is a simple example that
reads the metadata and a single image stack from an LLSM dataset and then
deskews the stack using information in the metadata. This flow is described
using a Free monad DSL can composes algerbras for reading metadata and images,
and processing images.

Let's get some imports out of the way first:

```tut:silent
import java.nio.file.{
  Path,
  Paths
}

import cats.~>
import cats.data.EitherK
import cats.free.Free
import cats.implicits._
import llsm.{
  Deskew,
  NoInterpolation
}
import llsm.algebras.{
  Metadata,
  MetadataF,
  ImgReader,
  ImgReaderF,
  Process,
  ProcessF
}
import llsm.interpreters._
import llsm.io.LLSMImg
import llsm.io.metadata.ConfigurableMetadata
import net.imglib2.img.cell.CellImgFactory
import net.imglib2.`type`.numeric.integer.UnsignedShortType
import org.scijava.Context
```

<br>
We will use a skewed PSF image dataset as as example. You could
replace this with a path to any raw LLSM stack. Note that stack should be in a
directory that also contains the LLSM instruments Settings file that holds a
lot of metadata.

```tut:silent
// Path to example dataset, which is incidentally a PSF.
val datasetPath: Path =
  Paths.get(
    getClass.getResource("/psf/psf642_ch0_stack0000_642nm_0000000msec_0008096612msecAbs.tif")
      .toURI
      .getPath
      )

// SCIFIO reading need a Scijava Context.
val scijavaContext = new Context()
```

<br>
Some metadata is not included in the metadata file from the LLSM acquisition
software. Thus we need to specify that here as `ConfigurableMetadata`. This
also includes the Interpolation method we want to use for deskewing.

```tut:silent
// The basicMetadataInterpreter requires some configurable metadata to be
// specified
val configMeta = ConfigurableMetadata(
    0.104, // xVoxelSize
    0.104, // yVoxelSize
    NoInterpolation //InterpolationMethod
)
```

<br>
Next we define a `EitherK` type that encapsulates the components/algerbras
of our intended application/program. In this case we need the `MetadataF`,
`ImgReaderF` and `ProcessF` algebras.

```tut:silent
type App[A] =
    EitherK[MetadataF,
      EitherK[ImgReaderF, ProcessF, ?],
    A]
```

<br>
Then we define program using our Free DSL that describes the flow of data for
the computation. It is parameterised on a type that has implicit Context
Bounds which allow the injection of 3 algebras we require. The Free injections are
aliased to `Metadata`, `ImgReader` and `Process` for readability.

```tut:silent
def program[F[_]: Metadata: ImgReader: Process](
    path: Path
): Free[F, LLSMImg] =
    for {
        meta    <- Metadata[F].readMetadata(path)
        img     <- ImgReader[F].readImg(path, meta)
        deskewedImg <- Process[F].deskewImg(
          img, 0,2,
          Deskew.calcShearFactor(
            meta.waveform.sPZTInterval,
            meta.sample.angle,
            meta.config.xVoxelSize),
          meta.config.interpolation)
    } yield deskewedImg
```

<br>
Next we define an interpreter for each algebra and combine them into an
overall interpreter that will execute our program. Some basic
interpreters for metadata and image reading using SCIFIO, and processing are
provided in the `llsm.interpreters` package. Importantly, the interpreters are
parameterised on an effect type where all "side"-effects are executed. In this case,
the effect type we use is an `Either[Throwable, ?]`. Exceptions will be
caught and recorded in the left side of the Either while successful execution will
result in the output being in the right side of the Either.

```tut:silent
def compiler =
  (basicMetadataInterpreter[Either[Throwable, ?]](configMeta, scijavaContext) or
    (scifioReaderInterpreter[Either[Throwable, ?]](scijavaContext, new CellImgFactory(new UnsignedShortType)) or
     processInterpreter[Either[Throwable, ?]]))
```

<br>
Finally, we execute the program with our compiler and match on the result. In
this case we don't actually do anything interesting with the deskewed `img`
result other than state we successfully processed it and then return it.

```tut:book
program[App](datasetPath).foldMap(compiler) match {
  case Right(LLSMImg(img, meta@_)) => {
    println("Successfully read metadata and img and then deskewed it")
    img
  }
  case Left(e) => println(s"Yikes there was an error: ${e.getMessage}")
}
```
