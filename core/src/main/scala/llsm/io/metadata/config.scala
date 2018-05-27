package llsm.io.metadata

import llsm.InterpolationMethod

case class ConfigurableMetadata(xVoxelSize: Double,
                                yVoxelSize: Double,
                                interpolation: InterpolationMethod)
