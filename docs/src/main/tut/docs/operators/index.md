---
layout: docs
title: LLS Acquisition
section: operators
position: 10
---

# LLS Data Acquisition Guidelines

In order to improve reproducibility and repeatability of Lattice Light-Sheet (LLS) data across different experiments and instruments, a series of parameters and metadata need to be captured. A lot of the metadata requirements are similar to typical widefield and laser scanning microscopes, but some are specific to the LLS microscope. This documentation contains information for LLS operators/users to guide them in acquiring all the necessary pieces of information to aid in:

1. Configuring an LLS instrument to reproduce similar imaging conditions as those used for a given dataset
2. Configuring software to reproduce preprocessing procedures for a processed dataset.

The guidelines are split into two parts. The [first part](reproducible.html) covers the general properties/metadata that should be captured for LLS experiments. These are not specific to a particular implementation of the LLS microscope.

The [second part](guidelines.html) covers the specific elements that need to be captured using the Janelia implementation of the LLS microscope in order to cover the properties/metadata outlined in the first section.

Note: These guidelines are still evolving. If you have suggestions for other
things that should be in the guidelines, we would encourage you to submit an
[issue](https://github.com/monash-merc/llsm/issues/new) on the `llsm` GitHub
repository.
