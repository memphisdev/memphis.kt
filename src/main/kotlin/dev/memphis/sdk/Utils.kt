package dev.memphis.sdk

import io.nats.client.impl.Headers
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal fun getInternalName(name: String): String = name.lowercase().replace(".", "#")

internal fun String.toInternalName() = getInternalName(this)

internal fun extendNameWithRandSuffix(name: String) =
    generateRandomHex(4).let { "${name}_$it" }


internal fun generateRandomHex(length: Int) =
    List(length * 2) { Random.nextInt(0, 16).toString(16) }.joinToString("")

internal fun Headers.toStringAll() = entrySet().joinToString(prefix = "(", postfix = ")") { "${it.key} -> ${it.value}" }
internal open class EnumOrdinalSerializer<E : Enum<E>>(
    private val kClass: KClass<E>,
    private val ordinalOffset: Int = 0
) : KSerializer<E> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(kClass.simpleName!!, PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: E) {
        encoder.encodeInt(value.ordinal + ordinalOffset)
    }

    override fun deserialize(decoder: Decoder): E {
        val ordinal = decoder.decodeInt()
        return kClass.java.enumConstants[ordinal - ordinalOffset]
    }
}

internal fun getDlsSubject(type: String, stationName: String, id: String) =
    "\$memphis-$stationName-dls.$type.$id"