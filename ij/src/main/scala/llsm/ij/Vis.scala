package llsm.ij

sealed trait Viewer
case object HyperStack extends Viewer
case object BigDataViewer extends Viewer
