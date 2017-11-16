---
layout: docs
title: Convert
section: fiji
---

# Process and Convert an LLS Dataset

This plugin allows a user to load, deskew and convert an LLSM dataset to either HDF5 or
OME-TIFF.

1. Run `Plugings → LLSM → Convert Dataset...`
2. Select an input directory containing a raw LLSM data (i.e., TIFF series +
metadata file).
3. Specify an output file and location. At this time the plugin only supports BigDataViewer HDF5 (extension: `.h5`) or OME-TIFF (extension: `.ome.tif`) as output formats.
4. Input deskewing parameters (Fig. 3 and Table 1).
5. Click OK.

<br>


![Convert Dataset](../../img/convert.jpg){: .img-responsive .center-block}

Figure 3: Convert Dataset Parameters GUI
{: .text-center}

<br>

_Table 1: Convert Parameters_

| Name | Parameter Description |
|: --- | :---------------------|
| Input | Path to input dataset folder |
| Output | Path to output file (valid extensions are `.h5` or `.ome.tif`) |
| X/Y Voxel Size | Physical X and Y voxel size |
| Img Container Type |  `imglib2` offers several Img container types, which have different performance characteristics (i.e., access rates and memory usage). The default is CellImg, which is conservative with memory usage but has some cost in access performance. ArrayImg offers the best access performance, but all image data is stored in memory |
| Interpolation Scheme<sup>1</sup> | Type of interpolation used for when shearing factor is not equivalent to the pixel size |

<sup>1</sup>Further information about interpolation schemes can be found in
[Deskew Information](deskew.html)
