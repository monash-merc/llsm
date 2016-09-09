package llsm
package io.metadata

case class Waveform(wfType: String, xGalvo: List[Waveform.Stage], zGalvo: List[Waveform.Stage], zPZT: List[Waveform.Stage], sPZT: List[Waveform.Stage], stackNo: List[Waveform.Stack], excitation: List[Waveform.Excitation], cycle: Waveform.Cycle, zmotion: Waveform.ZMotion)

object Waveform {
  case class Stage(channel: Int, offset: Double, interval: Double, excitationPixels: Int)
  case class Stack(channel: Int, number: Int)
  case class Excitation(channel: Int, filter: String, laser: Int, power: Int, exposure: Double)
  case class Cycle(mode: String)
  case class ZMotion(mode: String)
}
