package llsm.io.metadata

import java.util.UUID

case class FilenameMetadata(
    id: UUID,
    name: String,
    channel: Int,
    stack: Int,
    channelWavelength: Int,
    timeStamp: Long,
    absTimeStamp: Long
)
