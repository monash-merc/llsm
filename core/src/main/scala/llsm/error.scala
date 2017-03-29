package llsm

import llsm.io.metadata.LLSMMeta

sealed trait LLSMIOError
object LLSMIOError {
  case class MetadataIOError(error: LLSMMeta.MetadataError) extends LLSMIOError
  case class ImgIOError(message: String)                    extends LLSMIOError
}
