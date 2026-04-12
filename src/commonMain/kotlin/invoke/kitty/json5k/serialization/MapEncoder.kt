package invoke.kitty.json5k.serialization

import invoke.kitty.json5k.format.Token
import invoke.kitty.json5k.generation.FormatGenerator
import invoke.kitty.json5k.throwKeyTypeException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

internal class MapEncoder(parent: MainEncoder) : StructEncoder(parent) {
    private val keyEncoder: KeyEncoder = KeyEncoder(parent)

    init {
        generator.put(Token.BeginObject)
    }

    override fun getEncoderFor(descriptor: SerialDescriptor, index: Int): Encoder = if (index % 2 == 1) {
        super.getEncoderFor(descriptor, index)
    } else {
        keyEncoder
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        generator.put(Token.EndObject)
    }
}

private class KeyEncoder(parent: MainEncoder) : Encoder {
    override val serializersModule: SerializersModule = parent.serializersModule
    private val generator: FormatGenerator = parent.generator

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = throwKeyTypeException()
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = encodeString(enumDescriptor.getElementName(index))

    override fun encodeBoolean(value: Boolean) = encodeString(value.toString())
    override fun encodeByte(value: Byte) = encodeString(value.toString())
    override fun encodeShort(value: Short) = encodeString(value.toString())
    override fun encodeInt(value: Int) = encodeString(value.toString())
    override fun encodeLong(value: Long) = encodeString(value.toString())

    override fun encodeChar(value: Char) = encodeString(value.toString())

    override fun encodeFloat(value: Float) = encodeString(value.toString())
    override fun encodeDouble(value: Double) = encodeString(value.toString())

    @ExperimentalSerializationApi
    override fun encodeNull() = encodeString("null")

    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

    override fun encodeString(value: String) {
        generator.put(Token.MemberName(value))
    }
}
