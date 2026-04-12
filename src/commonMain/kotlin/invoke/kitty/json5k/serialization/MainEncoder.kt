package invoke.kitty.json5k.serialization

import invoke.kitty.json5k.Hexadecimal
import invoke.kitty.json5k.Json5
import invoke.kitty.json5k.Json5Array
import invoke.kitty.json5k.Json5Element
import invoke.kitty.json5k.Json5Encoder
import invoke.kitty.json5k.Json5Object
import invoke.kitty.json5k.Json5Primitive
import invoke.kitty.json5k.Settings
import invoke.kitty.json5k.format.Token
import invoke.kitty.json5k.generation.FormatGenerator
import invoke.kitty.json5k.isUnsignedNumber
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@OptIn(ExperimentalSerializationApi::class)
internal class MainEncoder(
    override val serializersModule: SerializersModule,
    val generator: FormatGenerator,
    val settings: Settings,
    override val json5: Json5
) : Encoder, Json5Encoder {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = when (descriptor.kind) {
        StructureKind.CLASS, StructureKind.OBJECT -> ClassEncoder(this)
        StructureKind.LIST -> ListEncoder(this)
        StructureKind.MAP -> MapEncoder(this)
        is PolymorphicKind -> PolymorphicEncoder(descriptor, this)
        else -> throw UnsupportedOperationException()
    }

    override fun encodeBoolean(value: Boolean) {
        generator.put(Token.Bool(value))
    }

    override fun encodeFloat(value: Float) = encodeDouble(value.toDouble())

    override fun encodeByte(value: Byte) = encodeLong(value.toLong())
    override fun encodeShort(value: Short) = encodeLong(value.toLong())
    override fun encodeInt(value: Int) = encodeLong(value.toLong())

    override fun encodeLong(value: Long) {
        generator.put(Token.Num(value.toString()))
    }

    override fun encodeDouble(value: Double) {
        generator.put(Token.Num(value.toString()))
    }

    override fun encodeString(value: String) {
        generator.put(Token.Str(value))
    }

    override fun encodeChar(value: Char) {
        generator.put(Token.Str(value.toString()))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        generator.put(Token.Str(enumDescriptor.getElementName(index)))
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder = if (descriptor.isUnsignedNumber) {
        UnsignedEncoder(this)
    } else {
        this
    }

    override fun encodeNull() {
        generator.put(Token.Null)
    }

    override fun encodeJson5Element(element: Json5Element) {
        when (element) {
            is Json5Array -> {
                generator.put(Token.BeginArray)
                for(value in element)
                    encodeJson5Element(value)
                generator.put(Token.EndArray)
            }
            is Json5Object -> {
                generator.put(Token.BeginObject)
                for((key, value) in element) {
                    if(element.comments.containsKey(key)) generator.writeComment(element.comments[key]!!)
                    generator.put(Token.MemberName(key))
                    encodeJson5Element(value)
                }
                generator.put(Token.EndObject)
            }
            is Json5Primitive -> {
                when(element.type) {
                    Json5Primitive.Type.STRING -> generator.put(Token.Str(element.content))
                    Json5Primitive.Type.BOOLEAN -> generator.put(Token.Bool(element.content.toBooleanStrict()))
                    Json5Primitive.Type.INTEGER,
                    Json5Primitive.Type.FLOAT -> generator.put(Token.Num(element.content))
                    Json5Primitive.Type.NULL -> generator.put(Token.Null)
                }
            }
        }
    }
}

@ExperimentalSerializationApi
private class UnsignedEncoder(private val parent: MainEncoder) : Encoder {
    override val serializersModule: SerializersModule = parent.serializersModule
    private val generator: FormatGenerator = parent.generator

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = throw UnsupportedOperationException()
    override fun encodeBoolean(value: Boolean) = throw UnsupportedOperationException()
    override fun encodeChar(value: Char) = throw UnsupportedOperationException()
    override fun encodeDouble(value: Double) = throw UnsupportedOperationException()
    override fun encodeFloat(value: Float) = throw UnsupportedOperationException()
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = throw UnsupportedOperationException()
    override fun encodeInline(descriptor: SerialDescriptor): Encoder = throw UnsupportedOperationException()
    override fun encodeString(value: String) = throw UnsupportedOperationException()

    private fun encodeUnsigned(value: ULong) {
        generator.put(Token.Num(value.toString()))
    }

    override fun encodeByte(value: Byte) = encodeUnsigned(value.toUByte().toULong())
    override fun encodeShort(value: Short) = encodeUnsigned(value.toUShort().toULong())
    override fun encodeInt(value: Int) = encodeUnsigned(value.toUInt().toULong())
    override fun encodeLong(value: Long) = encodeUnsigned(value.toULong())
    override fun encodeNull() = parent.encodeNull()
}

internal class HexNumberEncoder(
    private val parent: MainEncoder,
    private val hexFormat: HexFormat
) : Encoder by parent, Json5Encoder by parent {

    override fun encodeByte(value: Byte) {
        encodeHex(value.toHexString(hexFormat))
    }

    override fun encodeInt(value: Int) {
        encodeHex(value.toHexString(hexFormat))
    }

    override fun encodeLong(value: Long) {
        encodeHex(value.toHexString(hexFormat))
    }

    override fun encodeShort(value: Short) {
        encodeHex(value.toHexString(hexFormat))
    }

    private fun encodeHex(string: String) {
        parent.generator.put(Token.Num(string))
    }
}