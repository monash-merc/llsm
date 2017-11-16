---
layout: docs
title: Overview
section: develop
---

# Overview

The `llsm` project is divided into several sub-projects:

- [`core`](core.html) - Core I/O and processing functions with limited external
    dependencies.
- [`api`](api.html) - Free monad based API/DSL for composing programs that process and I/O
    LLS data
- `cli` - a command-line interface that allows preprocessing and conversion from raw LLSM datasets
    to OME-TIFF or BigDataViewer HDF5 datasets with all metadata encoded as an
    OME companion file.
- `ij` - A set of ImageJ/Fiji plugins that allows preprocessing and previewing/conversion of raw
    LLSM datasets.

