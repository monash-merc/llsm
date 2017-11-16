---
layout: docs
title: Reproducible
section: operators
---

# General Requirements for a Reproducible LLS Dataset

This page details the parameters/metadata that are generally required to reproduce imaging and processing conditions that were used for a given dataset. While this covers the minimal requirements for LLS datasets, we also recommend capturing as much other instrument and image metadata as possible that is compatible with the [Open Microscopy Environment (OME) model](https://docs.openmicroscopy.org/ome-model/5.5.7/).

## LLSM Specific Parameters/Metadata per Channel

1. Parameters used to generate an Lattice pattern for the Spatial Light Modulator (SLM):
   * Wavelength (nm)
   * Bessel beam number
   * Bessel beam spacing (um)
   * Inner NA
   * Outer NA
   * Cropping factor
   * Shift X (alignment specific)
   * Shift Y (alignment specific)
   * Tilt Angle (alignment specific)
2. SLM pattern binary mask (this is specific to the SLM hardware in the system so may not be readily transferrable to other instruments)
3. Annulus:
   * Inner NA
   * Outer NA
4. Laser:
   * Wavelength
   * Power
5. Emission filter description
6. PSF:
   * Captured using Sample Piezo scan i.e., skewed
   * Captured using Z Piezo/X Galvo scan
7. Objectives:
   * Illumination Objective - Mag, NA and back pupil diameter
   * Detection Objective - Mag and NA

## General Instrument/Image Metadata Requirements for Raw (Unprocessed) LLSM Datasets

1. Dimension sizes:
   * Width
   * Height
   * Depth (Z)
   * Channels
   * Time
2. Dimension Order
3. Bit Depth
4. Voxel size:
   * X
   * Y
5. Stage intervals (used to calculate Z interval/voxel size):
   * Sample Piezo
   * Z Piezo
   * X Galvo
   * Z Galvo
6. Objective angles relative to stage
7. Time:
   * Relative time stamp, i.e. 0s at start of experiment. Current LLSM implementation records the same time interval for XYZC stacks, i.e. each channel gets the same time stamp, even though channels are recorded sequentially. It may also  be useful to have a timestamp for each XYZ stack or even XY plane, as this could be used to do time correction.
   * Absolute time stamp
8. Other OME compatible image and instrument metadata

## Preprocessed Dataset Metadata Requirements

In addition to the general instrument/image metadata requirements above, the parameters and metadata required to reproduce preprocessing for a preprocessed dataset are as follows:

1. Voxel Size:
   * Z (calculated from objective angle, sample piezo interval and Z piezo interval)
2. Deskew parameters:
   * Shear dimension
   * Reference dimension
   * Shear factor (calculated from objective angle and sample piezo interval)
   * Interpolation method
3. Richardson-Lucy Deconvolution parameters:
   * Number of iterations
   * TV regularisation factor (if used)
   * PSF or PSF calculation parameters for non-measured PSFs

