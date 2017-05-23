---
layout: home
title: Home
section: "home"
---

`llsm` is a set of Scala libraries for working with Lattice Light-Sheet Microscopy (LLSM) data. Features include:

* Read and parse LLSM metadata from raw LLSM data.
* Read LLSM data.
* Deskew LLSM volumes.
* Output LLSM volumes as OME-TIFF or HDF5.

`llsm` is underpinned by Typelevel and ImageJ/Fiji community libraries including `cats`, `imglib2` and `scifio`.

# Getting Started
`llsm` is divided into several sub-projects:

* `core` processing libraries with limited dependencies (`imglib2`, `scifio`, `cats-core`).
* `api` a Free monad based API/DSL
* `ij` a set of plugins for ImageJ to accomplish some basic tasks.

# Note
`llsm` is currently in pre-release. You should expect some bugs.

# License
MIT

# Acknowledgements

`llsm` forms part of the [Australian National Data Service (ANDS)](http://www.ands.org.au/) Trusted Lattice Light-Sheet Microscopy data project. This project is a collaboration between:

- [Monash University](https://www.monash.edu)
- [University of New South Wales](https://www.unsw.edu.au)
- [University of Queensland](https://www.uq.edu.au)


Development of `llsm` is supported by:

- [Australian National Data Service (ANDS)](http://www.ands.org.au/)
- [Nectar](https://nectar.org.au/)


<div class="row">
	<div class="col-xs-6 col-md-4">
		<div class="logo_container">
			<div class="logo">
			<img src="img/cvl.jpg" />
			</div>
		</div>
	</div>
	<div class="col-xs-6 col-md-4">
		<div class="logo_container">
			<div class="logo">
			<img src="http://www.ands.org.au/__data/assets/image/0013/602311/ands-logo-transparent-background.png" />
			</div>
		</div>
	</div>
	<div class="col-xs-6 col-md-4">
		<div class="logo_container">
			<div class="logo">
			<img src="https://nectar.org.au/wp-content/uploads/2015/10/nectardirectorate-logo.png" />
			</div>
		</div>
	</div>
</div>
