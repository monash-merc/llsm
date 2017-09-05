---
layout: docs
title: Core Module
section: develop
---

# llsm core

The `core` sub-project contains many low level functions for reading and parsing of LLSM metadata and processing of LLSM raw dataset. This module doesn't provide many conveniences and most developers should aim to jump in at the [`api`](api.html) level.

## Metadata reading and parsing
Metadata for datasets derived from the Janelia LabView application is scattered in various places:

- _Settings.txt file in raw LLSM dataset folders.
- File name for each stack file.

`llsm` core provides a Parser typeclass for type-safe parsing of metadata from
these different sources into Scala case classes. Unfortunately metadata in the
settings file is relatively unstructured which make writing generic parsers a
challenge.
