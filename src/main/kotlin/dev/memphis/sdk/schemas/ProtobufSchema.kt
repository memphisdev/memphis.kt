package dev.memphis.sdk.schemas

import com.google.protobuf.DescriptorProtos
import dev.memphis.sdk.MemphisError
import dev.memphis.sdk.resources.SchemaVersion

internal class ProtobufSchema(
    name: String,
    activeVersion: SchemaVersion
) : Schema(name) {

    private val descriptor: DescriptorProtos.DescriptorProto

    init {
        val descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(activeVersion.descriptor.toByteArray())
        val fileName = "${name}_${activeVersion.number}.proto"

        val file = descriptorSet.fileList.find { it.name == fileName }
            ?: throw MemphisError("Descriptor '$fileName' not found ")

        descriptor = file.messageTypeList.find { it.name == activeVersion.messageStructName }
            ?: throw MemphisError("Message '${activeVersion.messageStructName}' not found")
    }

    override fun validateMessage(msg: ByteArray): ByteArray {
        try {
            descriptor.parserForType.parseFrom(msg)
        } catch (e: Exception) {
            throw MemphisError("Invalid message Format", e)
        }
        return msg
    }

}