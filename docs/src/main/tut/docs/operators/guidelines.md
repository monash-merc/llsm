---
layout: docs
title: LLS Dataset Guidelines
section: operators
---

# Guidelines for Acquiring Reproducible Datasets using the Janelia LLS Microscope

This section provides instructions for acquiring LLS datasets on the the original LLS microscope implementation by Chen et al. 2014, which uses the Janelia LLS LabView Acquisition software. These guidelines aim to capture all the metadata requirements specified in the previous section using the Janelia LLS microscope. In general, these guidelines suggest a specific layout of LLS data and some configuration files.

Instrument and image metadata for Janelia LLS raw datasets is primarily encoded in 2 locations:

* File names for each raw image stack
* Settings file

Metadata in the Settings files is relatively unstructured/unspecified.

An overview of the recommended layout for LLS data from a Janelia LLS microscope is shown in Fig. 1 below. The layout is experiment centric, where an “Experiment” encompasses a set of datasets for which the LLS configuration and channel setup were equivalent. For many users, Experiment might correspond to a single imaging session. This means creating a directory for an Experiment and hosting multiple datasets within that directory. Each dataset should be created in a separate directory inside the Experiment level. The Janelia LLS acquisition software defaults to outputting a 3D volume/stack for each channel and timepoint. It also outputs the Setting.txt file that holds hardware configuration details.

<br>

```
Experiment
  | —	dataset 1
  |	| — dataset 1_ch0_stack0000_488nm_0000msec_....tif
  |	| — dataset 1_ch0_stack0001_488nm_9749msec_....tif
  |	| — ...
  |	| — dataset 1_Settings.txt
  |
  | —	dataset 2
  |	| — dataset 2_ch0_stack0000_488nm_0000msec_....tif
  |	| — dataset 2_ch0_stack0001_488nm_9749msec_....tif
  |	| — ...
  |	| — dataset 2_Settings.txt
  |
  | —	dataset 3
  |	| — dataset 3_ch0_stack0000_488nm_0000msec_....tif
  |	| — dataset 3_ch0_stack0001_488nm_9749msec_....tif
  |	| — ...
  |	| — dataset 3_Settings.txt
  |
  | -	dataset n...
  |
  | —	configs
    | — 480nm_0.510_0.570_40_1.110_0.150_-2_0_-1.bmp
    | — 560nm_0.500_0.550_40_1.110_0.150_-2_0_-1.bmp
    | — config.json
```

Fig. 1. Data layout for an LLS experiment

<br>

## Configs

There are a number of parameters not explicitly captured by the Janelia LLSM acquisition software. Two methods are recommended for d capturing these:

* a config.json metadata file
* a bitmap image of the SLM pattern along with a specific encoding of SLM parameters in the bitmap file name.

Both of these configurable parameter metadata files should be place in a config subdirectory of an Experiment folder.

### Configurable Parameters - config.json

The config.json file is a [JSON formatted](https://www.json.org/) text file that explicitly captures important OME metadata that is not recorded by the Janelia acquisition software. This file can include only a minimal (required) set of properties (refer to Table 1. left column, below) or both the minimal set and an extended set of properties (Table 1. left and right columns). An example config.json file that includes the extended set of properties is shown in Fig. 2.

<br>

| Minimal (required)         | Extended (optional)                      |
| -------------------------- | ---------------------------------------- |
| xVoxelSize<br />yVoxelSize | lasers - a list of elements with following attributes:<br />- manufacturer<br />- model<br />- power<br />- powerUnit<br />- wavelength<br />- wavelengthUnit<br /><br />objectives - a list of elements with following attributes:<br />- manufacturer<br />- model<br />- lensNA<br />- magnification<br />- correction<br />- immersion<br /><br />filters - a list of elements with following attributes:<br />- manufacturer<br />- model<br />- filterType |

<br>

There are restrictions on the values for the following Extended properties: laser [powerUnit](http://www.openmicroscopy.org/Schemas/Documentation/Generated/OME-2016-06/ome_xsd.html#LightSource_PowerUnit), laser [wavelengthUnit](http://www.openmicroscopy.org/Schemas/Documentation/Generated/OME-2016-06/ome_xsd.html#Laser_WavelengthUnit), objective [correction](http://www.openmicroscopy.org/Schemas/Documentation/Generated/OME-2016-06/ome_xsd.html#Objective), objective [immersion](http://www.openmicroscopy.org/Schemas/Documentation/Generated/OME-2016-06/ome_xsd.html#Objective) and filter [filterType](http://www.openmicroscopy.org/Schemas/Documentation/Generated/OME-2016-06/ome_xsd.html#Filter_Type). These restrictions are enumerated in the [OME model specification](http://www.openmicroscopy.org/Schemas/Documentation/Generated/OME-2016-06/ome.html).

<br>

```json
{
    "voxelSizeX": 0.1018,
    "voxelSizeY": 0.1018,
    "lasers": [
        {
            "manufacturer": "Coherent",
            "model": "OBIS",
            "power": 100,
            "powerUnits": "mW",
            "wavelength": 488,
            "wavelengthUnit": "nm"
        },
        {
            "manufacturer": "Coherent",
            "model": "OBIS",
            "power": 100,
            "powerUnits": "mW",
            "wavelength": 561,
            "wavelengthUnit": "nm"
        }
    ],
    "objectives": [
        {
            "manufacturer": "Nikon",
            "model": "20x PlanApo",
            "lensNA": 1.0,
            "magnification": "20x",
            "correction": "PlanApo",
            "path": "detection"
        },
        {
            "manufacturer": "Nikon",
            "model": "10x PlanApo",
            "lensNA": 1.0,
            "magnification": "20x",
            "correction": "PlanApo",
            "path": "illumination"
        },
    ],
    "filters": [
        {
            "manufacturer": "Filter Inc.",
            "model": "460 LP",
            "filterType": "LongPass"
        }
    ]
}
```
Fig. 2. Example config.json

<br>

### Lattice Pattern

Recreating a lattice pattern across different instrument requires several parameters (as described in Section 1). In the case of the Janelia LLS microscope implementation, the SLM hardware model is similar and therefore bitmap patterns can be transferred from instrument to instrument. Nevertheless, to ensure all metadata is captured, we recommend encoding the Lattice pattern parameters into the filename of the bitmap image according to the following underscore separated specification.

> Wave_Inner_Outer_Beam#_Spacing_Crop_ShiftX_ShiftY_Tilt.bmp

A description of the terms, separated by underscores, is given in Table 2. An example file name might look like:

> 480nm_0.510_0.570_40_1.110_0.150_-2_0_-1.bmp

<br>

| **Name** | **Description**                          |
| -------- | ---------------------------------------- |
| Wave     | The excitation wavelength (in nanometers) |
| Inner    | Inner diameter (NA) of the annular mask  |
| Outer    | Outer diameter (NA) of the annular mask  |
| Beam#    | Number of bessel beams across the sheet. Controls width of sheet. |
| Spacing  | Spacing between individual bessel beams (in microns) |
| Crop     | Binary threshold factor, based on the amplitude of the electric field at the sample |
| ShiftX   | Shift of the pattern in the X direction on the SLM (in microns) |
| ShiftY   | Shift of the pattern in the Y direction on the SLM (in microns) |
| Tilt     | Rotation of the pattern clockwise (in degrees) |

Table 2. SLM Pattern Generator Parameters

<br>

## Including a Measured Point Spread Function (PSF)

While PSFs are not strictly necessary unless one wants to perform deconvolution with a measured PSF, they can be useful for assessing the quality of the imaging setup. They are very useful for diagnosing issues with the alignment of an LLS microscope and for setting final offsets. They can also reveal problems with the objectives. For these reasons, performing regular PSF measurements is recommended. In general, new PSF measurements for each Experiment is suggested, but at the very least for each change in the SLM pattern or realignment of the system.

There are two suggested schemes for including PSF data. The choice of which to use depends on the manner in which the PSFs were acquired. In both cases the PSF data should be output to a subdirectory of the Experiment level called “psf.” Fig. 3 shows a scheme in which all PSFs are measured on the same sub-resolution beads, i.e. a single dataset using with all channels. Fig. 4 shows a scheme in which measured PSFs were acquired for each channel separately, i.e. on different sets of beads.

<br>

```
Experiment
	| —	psf
	|	| - psf_ch0_stack0000_488nm_0000000msec_....tif
	|	| - psf_ch1_stack0000_561nm_0000000msec_....tif
	|	| - psf_ch2_stack0000_642nm_0000000msec_....tif
	|	| - ...
	|	| - psf_Settings.txt
	|
	| —	dataset 1
	|	| —	dataset 1_ch0_stack0000_488nm_0000msec_....tif
	|	| —	dataset 1_ch0_stack0001_488nm_9749msec_....tif
	|	| — ...
	|	| — dataset 1_Settings.txt
	|
	| —	configs
		| — 480nm_0.510_0.570_40_1.110_0.150_-2_0_-1.bmp
		| — 560nm_0.500_0.550_40_1.110_0.150_-2_0_-1.bmp
		| — config.json
```

Fig. 3. Data layout for an LLSM experiment with PSFs for each channel acquired with the same settings on the same sub-resolution beads, e.g. TetraSpeck micro beads.

<br>

```
Experiment
	| —	psf
	|	| - psf488
	|	| - psf488_ch0_stack0000_488nm_0000000msec_....tif
	|	| - psf488_Settings.txt
	|
	|	| - psf561
	|	| - psf561_ch0_stack0000_561nm_0000000msec_....tif
	|	| - psf561_Settings.txt
	|
	| -	psfn
		| - ...
	|
	| —	dataset 1
	|	| —	dataset 1_ch0_stack0000_488nm_0000msec_....tif
	|	| —	dataset 1_ch0_stack0001_488nm_9749msec_....tif
	|	| — ...
	|	| — dataset 1_Settings.txt
	|
	| —	configs
		| — 480nm_0.510_0.570_40_1.110_0.150_-2_0_-1.bmp
		| — 560nm_0.500_0.550_40_1.110_0.150_-2_0_-1.bmp
		| — config.json

```

Fig. 4. Data layout for an LLSM experiment with PSFs for each channel acquired with different settings on different beads.
