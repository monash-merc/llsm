---
layout: docs
title: ImageJ/Fiji Plugins
menu_section: users
---

# Introduction
Below we described basic usage of the `llsm` ImageJ/Fiji plugins. We assume that the `llsm` plugins have been installed as described in the [Getting Started](index.html/#LLSMPluginInstallation) section.

## Deskew and preview a single stack
This plugin is generally not all that useful but does give you a quick way to
deskew and visualise a single stack (i.e., timepoint or channel) without loading the whole
dataset.

1. Start by opening a single stack in Fiji.
2. Run `Plugins → LLSM → Deskew Single Stack`
3. Enter parameters for deskewing (see Fig. 1)
4. Click OK.

<br>

_Table 1: Deskew Single Stack Parameters_

| Name | Parameter Description |
|: --- | :---------------------|
| X/Y Voxel Size | Physical size of X and Y in voxel |
| Sample Piezo Increment | Interval of the sample piezo stage for each Z slice |
| Img Container Type |  `imglib2` offers several Img container types. Each have different performance characteristics in terms of access rates and memory usage. By default the CellImg is used, which is conservative with memory usage but has some cost in performance. ArrayImg offer best access performance, but all image data is stored in memory |
| Interpolation Scheme<sup>1</sup> | Type of interpolation used for when shearing factor is not equivalent to the pixel size |

<sup>1</sup>Further information about interpolation schemes can be found in
[Deskew Information](deskew.html)

<br>

![Deskew Single Stack](../../img/deskew_single.jpg){: .img-responsive .center-block}

Figure 1: Deskew Single Stack Parameters GUI
{: .text-center}

---

## Deskew and preview a LLSM dataset (time series)

This plugin allows one to load, deskew and preview an entire LLSM dataset.

1. Run `Plugins → LLSM → Deskew Time Series`
2. Select an input directory
3. Input deskewing parameters (Fig 2. and Table 2).
3. Click OK.

<br>

_Table 2: Deskew Parameters_

| Name | Parameter Description |
|: --- | :---------------------|
| X/Y Voxel Size | Physical size of X and Y in voxel |
| Incident Objective Angle | Angle between the stage and the illumination objective. |
| Img Container Type |  `imglib2` offers several Img container types. Each have different performance characteristics in terms of access rates and memory usage. By default the CellImg is used, which is conservative with memory usage but has some cost in performance. ArrayImg offer best access performance, but all image data is stored in memory |
| Interpolation Scheme<sup>1</sup> | Type of interpolation used for when shearing factor is not equivalent to the pixel size |

<sup>1</sup>Further information about interpolation schemes can be found in
[Deskew Information](deskew.html)

<br>

![Deskew Time Series](../../img/deskew_time.jpg){: .img-responsive .center-block}

Figure 2: Deskew Time Series Parameters GUI
{: .text-center}

---

## Convert LLSM dataset

Allows one to load, deskew and output an LLSM dataset as either an HDF5 or
OME-TIFF.

1. Run `Plugings → LLSM → Convert Time Series`
2. Select an input directory contain raw LLSM data (i.e., TIFF series +
metadata file).
3. Select an output directory.
4. Input an output file name with either a `.h5` or `.ome.tif` extension.
5. Input deskewing parameters (Fig. 3 and Table 2).
6. Click OK.

<br>


![Convert Time Series](../../img/convert.jpg){: .img-responsive .center-block}

Figure 3: Convert Time Series Parameters GUI
{: .text-center}
