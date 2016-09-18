package llsm
package io.metadata

case class Waveform(wfType: String, channels: List[Waveform.Channel], cycle: String, zmotion: String)

object Waveform {
  case class Type(name: String)
  case class Channel(id: Int, stack: Int, xGalvo: Stage, zGalvo: Stage, zPZT: Stage, sPZT: Stage, excitation: Excitation)
  case class Stage(channel: Int, offset: Double, interval: Double, excitationPixels: Int)
  case class Stack(channel: Int, number: Int)
  case class Excitation(channel: Int, filter: String, laser: Int, power: Int, exposure: Double)
  case class Cycle(mode: String)
  case class ZMotion(mode: String)
}
