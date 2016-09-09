package llsm
package ij

import llsm.Deskew

import net.imagej.Dataset
import net.imagej.ops.OpService
import net.imglib2.RandomAccessible
import net.imglib2.img.Img
import net.imglib2.`type`.numeric.RealType
import org.scijava.ItemIO
import org.scijava.command.Command
import org.scijava.plugin.{Plugin, Parameter}

@Plugin(`type` = classOf[Command], headless = true, menuPath = "Plugins>LLSM>Deskew Image (Real)")
class RealDeskewPlugin extends Command {

  @Parameter(`type` = ItemIO.INPUT)
  var input: Dataset = _

  @Parameter
  var ops: OpService = _

  @Parameter(`type` = ItemIO.OUTPUT)
  var output: RandomAccessible[_] = _

  /**
  * Entry point to running a plugin.
  */
  override def run(): Unit =  {
    val ip: Img[T] forSome {type T <: RealType[T]} = input.getImgPlus.getImg.asInstanceOf[Img[T] forSome {type T <: RealType[T]}]

    output = Deskew.deskewRealStack(ip, 0, 2, 2.94)
  }
}
