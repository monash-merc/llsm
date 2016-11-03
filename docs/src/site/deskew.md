---
layout: "default"
title: "De-skewing"
section: "Preprocessing"
source: "core/src/main/scala/llsm/Deskew.scala"
scalaDoc: "#llsm.Deskew"
---

# De-skewing LLSM data
What is de-skewing and why do we need to do it?


## Implementation
De-skewing of LLSM data is implemented using using a shear transform. Transforms are performed using the `imglib2` library; however, the implementation provided by `imglib2` currently only supports simple shear transforms with a shear factor of 1. Therefore, we implemented our only versions that allow shear transforms with arbitrary shear factors.
There are actually 2 versions available:
1. Simple bounding box transform.
2. Real view of data with interpolation combined with a bounding box transform.

The difficult thing about LLSM data is that the stage offsets used to generate
Z sections are seldom divisible exactly by the physical size of a pixel in X (i.e., 101.8 nm), meaning that one cannot simply shear using whole pixel units. Therefore, a compromise is required, which can be broadly classified into to approaches:
1. We could simply shift by the closest whole pixel unit to the stage offset.
2. Interpolate the pixel data such that we can resample the data at the correct locations.

## Performance Characteristics
Performance of the de-skewing is largely driven by the interpolation scheme used. The simple bounding box transform without interpolation is the fastest, although interpolation using a simple NearestNeighbour algorithms is only 1.42x slower in our measurements. Linear interpolation is about 7x slower than the bounding box transform, while Lanczos is orders of magnitude slower.

You can see the details of benchmarking [here](deskew_benchmark.html)

