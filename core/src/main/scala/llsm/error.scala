package llsm

import llsm.io.metadata.FileMetadata

sealed trait LLSMIOError
object LLSMIOError {
  case class MetadataIOError(error: FileMetadata.MetadataError) extends LLSMIOError
  case class ImgIOError(message: String)                    extends LLSMIOError
}
