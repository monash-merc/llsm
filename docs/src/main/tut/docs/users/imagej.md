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
2. Enter input directory and deskewing parameters (Fig 2. and Table 2).
3. Click OK.

<br>

_Table 2: Deskew Time Series Parameters_

| Name | Parameter Description |
|: --- | :---------------------|
| X/Y Voxel Size | Physical size of X and Y in voxel |
| Incident Objective Angle | Angle between the stage and the illumination objective. |
| Interpolation Scheme<sup>1</sup> | Type of interpolation used for when shearing factor is not equivalent to the pixel size |

<br>

![Deskew Time Series](../../img/deskew_time.jpg){: .img-responsive .center-block}

Figure 2: Deskew Time Series Parameters GUI
{: .text-center}

---

## Convert LLSM dataset

Allows one to load, deskew and output an LLSM dataset as either an HDF5 or
OME-TIFF.

_Documentation to follow..._

