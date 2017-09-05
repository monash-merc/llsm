---
layout: docs
title: Preview
section: fiji
---

# Preview LLSM dataset

Plugin to load, process (e.g., deskew) and preview an entire LLSM dataset.

1. Run `Plugins → LLSM → Preview Dataset...`
2. Select an input directory containing a raw LLSM data (i.e., TIFF series +
metadata file).
3. Input deskewing parameters (Fig 2. and Table 2).
3. Click OK.

<br>

![Preview Dataset](../../img/preview_dataset.jpg){: .img-responsive .center-block}

Figure 1: Preview Dataset Parameters GUI
{: .text-center}

<br>

_Table 1: Preview Dataset Parameters_

| Name | Parameter Description |
|: --- | :---------------------|
| Input | Path to input dataset folder |
| X/Y Voxel Size | Physical X and Y voxel size |
| Img Container Type |  `imglib2` offers several Img container types, which have different performance characteristics (i.e., access rates and memory usage). The default is CellImg, which is conservative with memory usage but has some cost in access performance. ArrayImg offers the best access performance, but all image data is stored in memory |
| Interpolation Scheme<sup>1</sup> | Type of interpolation used for when shearing factor is not equivalent to the pixel size |
| Preview in BigDataViewer | Flag to specify whether to preview the dataset in the BigDataViewer or in standard ImageJ HyperStack. |

<sup>1</sup>Further information about interpolation schemes can be found in
[Deskew Information](deskew.html)

---

# Deskew and preview a single stack
This plugin is generally not all that useful but does give you a quick way to
deskew and visualise a single stack (i.e., timepoint or channel) without loading the whole
dataset.

1. Start by opening a single stack in Fiji.
2. Run `Plugins → LLSM → Deskew Single Stack`
3. Enter parameters for deskewing (see Fig. 2 and Table 2)
4. Click OK.

<br>

![Deskew Single Stack](../../img/deskew_single.jpg){: .img-responsive .center-block}

Figure 2: Deskew Single Stack Parameters GUI
{: .text-center}

<br>

_Table 2: Deskew Single Stack Parameters_

| Name | Parameter Description |
|: --- | :---------------------|
| X/Y Voxel Size | Physical size of X and Y in voxel |
| Sample Piezo Increment | Interval of the sample piezo stage for each Z slice |
| Img Container Type |  `imglib2` offers several Img container types. Each have different performance characteristics in terms of access rates and memory usage. By default the CellImg is used, which is conservative with memory usage but has some cost in performance. ArrayImg offers best access performance, but all image data is stored in memory |
| Interpolation Scheme<sup>1</sup> | Type of interpolation used for when shearing factor is not equivalent to the pixel size |

<sup>1</sup>Further information about interpolation schemes can be found in
[Deskew Information](deskew.html)

---

