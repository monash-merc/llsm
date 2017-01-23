package llsm
package io.metadata

case class WaveformMetadata(wfType: String,
                            nSlices: Long,
                            zPZTOffset: Double,
                            sPZTOffset: Double,
                            channels: List[WaveformMetadata.Channel],
                            nFrames: Int,
                            cycle: String,
                            zmotion: String)

object WaveformMetadata {
  case class Type(name: String)
  case class Channel(id: Int,
                     stack: Int,
                     xGalvo: Stage,
                     zGalvo: Stage,
                     zPZT: Stage,
                     sPZT: Stage,
                     excitation: Excitation)
  case class Stage(channel: Int,
                   offset: Double,
                   interval: Double,
                   excitationPixels: Long)
  case class Stack(channel: Int, number: Int)
  case class Excitation(channel: Int,
                        filter: String,
                        laser: Int,
                        power: Int,
                        exposure: Double)
  case class Cycle(mode: String)
  case class ZMotion(mode: String)
}
