# Changelog

## Version 0.4.0

* Enabled WartRemover for core, api and tests. Removed many Scala warts which
    should mean more stable and bug free libraries.
* Renamed Fiji plugin to better reflect their function. Preview... is used to
    preview LLSM raw datasets, while Convert... is used to convert from raw
    dataset to BDV HDF5 or OME-TIFF.
* Preview directly in the BigDataViewer for ImageJ/Fii preview plugin.
* Added Visualise Algebra/DSL and refactored to plugins so that all programs
    can be implemented using Free monads.
