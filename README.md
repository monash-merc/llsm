[![Build Status](https://api.travis-ci.org/monash-merc/llsm.png)](https://travis-ci.org/monash-merc/llsm/)

# llsm
`llsm` is a set of Scala libraries for preprocessing and conversion of raw data
from early implementations of the Lattice Light-sheet Microscope<sup>1</sup>.

Features include:

* Read and parse LLSM metadata from raw LLSM data from the Janelia LabView
    application.
* Read LLSM raw data from the Janelia LabView application.
* Deskew LLSM volumes.
* Output LLSM volumes as OME-TIFF or BigDataViewer HDF5 formats.

`llsm` is underpinned by Typelevel and ImageJ/Fiji community libraries including `cats`, `imglib2` and `scifio`.

__Note: Please be aware that these libraries are still in heavy development and are likely to change substantially.__

<sup>1</sup>Chen, B.-C., Legant, W. R., Wang, K., Shao, L., Milkie, D. E., Davidson, M. W., et al. (2014). Lattice light-sheet microscopy: imaging molecules to embryos at high spatiotemporal resolution. *Science* (New York, N.Y.), 346(6208), 1257998–1257998. [http://doi.org/10.1126/science.1257998](http://doi.org/10.1126/science.1257998)

## Acknowledgements
`llsm` forms part of the [Australian National Data Service (ANDS)](http://www.ands.org.au/) Trusted Lattice Light-Sheet Microscopy data project. This project is a collaboration between:

- [Monash University](https://www.monash.edu)
- [University of New South Wales](https://www.unsw.edu.au)
- [University of Queensland](https://www.uq.edu.au)


Development of `llsm` is supported by:

- [Australian National Data Service (ANDS)](http://www.ands.org.au/)
- [Nectar](https://nectar.org.au/)

## License
This code is licensed under the [MIT license](https://opensource.org/licenses/MIT).
