---
layout: docs
title: Benchmarks
---
Benchmark different interpolation methods for a shear transform.

The benchmarks below use fake images 100 x 100 x 100 voxels generated using SCIFIO and were run on a:

- MacBook Pro (13-inch, 2016, Four Thunderbolt 3 Ports)
- 3.3 GHz Intel Core i7
- 16 GB 2133 MHz LPDDR3

```
> jmh:run -i 10 -wi 10 -f 2 -t 1 .*DeskewBenchmark.*

...

[info] # Run complete. Total time: 00:12:12
[info]
[info] Benchmark                                      Mode  Cnt   Score   Error  Units
[info] FakeDeskewBenchmark.deskewBBTransform          avgt   20   0.113 ± 0.001   s/op
[info] FakeDeskewBenchmark.deskewLanczosInt           avgt   20  14.065 ± 0.583   s/op
[info] FakeDeskewBenchmark.deskewNLinearInt           avgt   20   0.703 ± 0.022   s/op
[info] FakeDeskewBenchmark.deskewNearestNeighbourInt  avgt   20   0.154 ± 0.005   s/op
[success] Total time: 736 s, completed 19/04/2017 3:01:17 PM
```
