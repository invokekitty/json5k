package invoke.kitty.json5k.deserialization

import invoke.kitty.json5k.DuplicateKeyError
import invoke.kitty.json5k.format.Token
import invoke.kitty.json5k.parsing.LookaheadParser
import invoke.kitty.json5k.throwKeyTypeException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

internal class MapDecoder(parent: MainDecoder) : StructDecoder(parent, Token.BeginObject) {
    private val keyDecoder = KeyDecoder(parent)
    private var count: Int = 0

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T = if (index % 2 == 0) {
        deserializer.deserialize(keyDecoder)
    } else {
        super.decodeSerializableElement(descriptor, index, deserializer, previousValue)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val token = parser.peek().item
        return if (token is Token.EndObject) {
            CompositeDecoder.DECODE_DONE
        } else {
            count++
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        parser.next()
    }
}

private class KeyDecoder(private val parent: MainDecoder) : Decoder {
    override val serializersModule: SerializersModule = parent.serializersModule
    private val specifiedKeys: MutableSet<String> = mutableSetOf()
    private val parser: LookaheadParser<Token> = parent.parser

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = throwKeyTypeException()
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = enumDescriptor.getElementIndex(decodeString())

    override fun decodeBoolean(): Boolean = decodeString().toBooleanStrict()

    override fun decodeByte(): Byte = decodeString().toByte()
    override fun decodeShort(): Short = decodeString().toShort()
    override fun decodeInt(): Int = decodeString().toInt()
    override fun decodeLong(): Long = decodeString().toLong()

    override fun decodeChar(): Char {
        val str = decodeString()
        require(str.length == 1)
        return str[0]
    }

    override fun decodeFloat(): Float = decodeString().toFloat()
    override fun decodeDouble(): Double = decodeString().toDouble()

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean = decodeString() == "null"

    @ExperimentalSerializationApi
    override fun decodeNull() = null

    override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

    override fun decodeString(): String {
        val (pos, token) = parser.next()
        check(token is Token.MemberName)

        val name = token.name
        if (!specifiedKeys.add(name)) {
            throw DuplicateKeyError(name, pos)
        }

        return name
    }
}
