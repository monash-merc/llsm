---
layout: docs
title: LLS Processing
section: fiji
position: 20
---

# Preprocessing of LLS Data
LLS data typically requires some preprocessing before it can be visualised, analysed or
processed further. Preprocessing usually encompasses deskewing and capture
of important metadata that it useful for downstream applications. More
information about deskewing LLS data can be found [here](deskew.html).

`llsm` provides several plugins for ImageJ/Fiji that enable preprocessing LLS
data. The main features of these plugins include:

* Reading and parsing LLS metadata from raw LLS data from the Janelia LabView
    application.
* Reading LLS raw data from the Janelia LabView application.
* Deskewing LLS volumes.
* Previewing LLS datasets as ImageJ HyperStacks or in the BigDataViewer.
* Exporting or conversion of LLS dataset to OME-TIFF or BigDataViewer HDF5
    formats.

The plugins cover two primary use cases at this time, although we wish to
support more in the future:

- [Process and preview LLS datasets](preview.html)
- [Process and convert LLS datasets to OME-TIFF or BigDataViewer HDF5
    formats](convert.html)

Note: While we aim to support other LLS microscope implementations in the future, the
`llsm` tools currently only support datasets acquired using the Janelia LLS
acqusition software. The plugins typically operate directly on the raw data
structure generated from the Janelia LLS acquisition software. If you are interested in guidelines for acquiring LLS datasets
proceed [here](../operators/index.html) or if you are interested in developing or using
`llsm` libraries proceed [here](../dev/index.html).


## Installing the ImageJ/Fiji Plugins
There is an ImageJ/Fiji update site for `llsm`, so installing the plugins is
relatively straight forward:

1. [Download](https://imagej.net/Fiji/Downloads) and install Fiji.

2. Add and enable the `llsm` Fiji Update Site:

    1. Open the Fiji updater: Help → Update ...
    2. Click "Manage update sites"
    3. Click "Add update site" and enter the `llsm` update site with the following parameters:

        Name: LLSM

        URL: [http://sites.imagej.net/LLSM/](http://sites.imagej.net/LLSM/)

        ![Update site](../../img/update_site.jpg)
    4. Ensure it is enabled and click "Close"
    5. Check that ‘plugins/llsm-ij.jar’ appears with the status “Install It” in the updater dialog
    6. Click ‘Apply Changes’


4. Restart Fiji.

5. Verify that you have an `LLSM` submenu under the Plugins menu in Fiji.
