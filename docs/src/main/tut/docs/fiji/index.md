---
layout: docs
title: Fiji Plugins
section: fiji
position: 10
---

# Getting Started
This section describes usage of the ImageJ/Fiji plugins for processing LLSM
data. The plugins are targeted at end-users and can generally classified into 2
groups:

- [Previewing LLSM datasets](preview.html)
- [Converting LLSM datasets to OME-TIFF or BigDataViewer HDF5
    formats](convert.html)

If you are interested in general guidelines for LLSM instrument operators please
proceed [here](../operators.html) or if you are interested in developing or using
`llsm` libraries please go [here](../dev/index.html).


## LLSM Plugin Installation

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
