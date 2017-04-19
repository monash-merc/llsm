[![Build Status](https://api.travis-ci.org/keithschulze/llsm.png)](https://travis-ci.org/keithschulze/llsm/)

# llsm
`llsm` is a set of Scala libraries for preprocessing and conversion of raw data
from early implementations of the Lattice Light-sheet Microscope<sup>1</sup>.

Features include:

* Read and parse LLSM metadata from raw LLSM data.
* Read LLSM data.
* Deskew LLSM volumes.
* Output LLSM volumes as OME-TIFF.

`llsm` is underpinned by Typelevel and ImageJ/Fiji community libraries including `cats`, `imglib2` and `scifio`.

__Note: Please be aware that these libraries are still in heavy development and are likely to change substantially.__

<sup>1</sup>Chen, B.-C., Legant, W. R., Wang, K., Shao, L., Milkie, D. E., Davidson, M. W., et al. (2014). Lattice light-sheet microscopy: imaging molecules to embryos at high spatiotemporal resolution. *Science* (New York, N.Y.), 346(6208), 1257998–1257998. [http://doi.org/10.1126/science.1257998](http://doi.org/10.1126/science.1257998)

## Acknowledgements
Development of `llsm` is supported by:

* Monash University
* Australian National Data Service (ANDS)

It is also part of the ANDS Trusted Lattice Light-Sheet Microscopy data project.

## License
This code is licensed under the [MIT license](https://opensource.org/licenses/MIT).
