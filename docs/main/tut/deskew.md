---
layout: "default"
title: "De-skewing"
section: "Preprocessing"
source: "core/src/main/scala/llsm/Deskew.scala"
scalaDoc: "#llsm.Deskew"
---

# De-skewing LLSM data

## Implementation
De-skewing of LLSM data is implemented using the ImgLib2 transforms and views.
There are actually several different transforms available, but they can be
broadly classified into 2 groups:
1. Simple bounding box transform
2. Real view of data with interpolation combined with a bounding box transform.

The difficult thing about LLSM data is that the stage offsets used to generate
Z sections is seldom a whole pixel unit (i.e., 101.8 nm), which means that

## Performance Characteristics

