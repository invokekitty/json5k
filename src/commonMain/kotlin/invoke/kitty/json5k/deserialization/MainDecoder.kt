package invoke.kitty.json5k.deserialization

import invoke.kitty.json5k.DecodingError
import invoke.kitty.json5k.Json5
import invoke.kitty.json5k.Json5Array
import invoke.kitty.json5k.Json5Decoder
import invoke.kitty.json5k.Json5Element
import invoke.kitty.json5k.Json5Literal
import invoke.kitty.json5k.Json5Null
import invoke.kitty.json5k.Json5Object
import invoke.kitty.json5k.Json5Primitive
import invoke.kitty.json5k.MissingFieldError
import invoke.kitty.json5k.Settings
import invoke.kitty.json5k.UnexpectedValueError
import invoke.kitty.json5k.format.Token
import invoke.kitty.json5k.isUnsignedNumber
import invoke.kitty.json5k.parsing.InjectableLookaheadParser
import invoke.kitty.json5k.parsing.LinePosition
import invoke.kitty.json5k.parsing.LookaheadParser
import invoke.kitty.json5k.parsing.Parser
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal class MainDecoder(
    override val serializersModule: SerializersModule,
    internal val parser: InjectableLookaheadParser<Token>,
    val settings: Settings,
    override val json5: Json5
) : Decoder, Json5Decoder {
    private var beginPos: LinePosition? = null
    private var polymorphicDecoder: PolymorphicDecoder? = null

    constructor(other: MainDecoder) : this(other.serializersModule, other.parser, other.settings, other.json5)

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        try {
            val res = super.decodeSerializableValue(deserializer)
            beginPos = null
            polymorphicDecoder = null
            return res
        } catch (e: MissingFieldException) {
            // Throw the library version of a MissingFieldException (for consistency):
            val firstField = e.missingFields[0]
            throw MissingFieldError(firstField, beginPos!!)
        } catch (e: SerializationException) {
            if (polymorphicDecoder != null && e.message?.contains("polymorphic serialization") == true) {
                val classNameToken = polymorphicDecoder?.classNameToken
                if (classNameToken != null) {
                    // Assume/infer that the error is (probably) caused by a missing serializer:
                    val className = classNameToken.item.string
                    throw UnexpectedValueError("unknown class name '$className'", classNameToken.pos)
                }
            }

            throw e
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        beginPos = parser.peek().pos
        return when (descriptor.kind) {
            StructureKind.CLASS -> if (polymorphicDecoder != null) {
                ClassDecoder(this, setOf(polymorphicDecoder!!.classDiscriminator))
            } else {
                ClassDecoder(this)
            }

            StructureKind.LIST -> ListDecoder(this)
            StructureKind.MAP -> MapDecoder(this)
            StructureKind.OBJECT -> ObjectDecoder(this)
            is PolymorphicKind -> {
                val decoder = PolymorphicDecoder(descriptor, this)
                polymorphicDecoder = decoder
                decoder
            }

            else -> throw UnsupportedOperationException()
        }
    }

    override fun decodeByte(): Byte = parser.getInteger(SignedLimits.BYTE).toByte()
    override fun decodeShort(): Short = parser.getInteger(SignedLimits.SHORT).toShort()
    override fun decodeInt(): Int = parser.getInteger(SignedLimits.INT).toInt()
    override fun decodeLong(): Long = parser.getInteger(SignedLimits.LONG)

    override fun decodeDouble(): Double = parser.getDouble()
    override fun decodeFloat(): Float = decodeDouble().toFloat()

    override fun decodeBoolean(): Boolean = parser.next().extractType<Token.Bool>().bool
    override fun decodeString(): String = parser.next().extractType<Token.Str>().string

    override fun decodeChar(): Char {
        val (pos, token) = parser.next().mapType<Token.Str>()
        if (token.string.length != 1) {
            throw UnexpectedValueError("single-character string expected", pos)
        }

        return token.string[0]
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val (pos, token) = parser.next().mapType<Token.Str>()

        val value = token.string
        val index = enumDescriptor.getElementIndex(value)

        if (index == CompositeDecoder.UNKNOWN_NAME) {
            throw UnexpectedValueError("unexpected enum value '$value'", pos)
        }

        return index
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder = if (descriptor.isUnsignedNumber) {
        UnsignedDecoder(this)
    } else {
        this
    }

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean {
        return parser.peek().item !is Token.Null
    }

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing? {
        parser.next().extractType<Token.Null>()
        return null
    }

    override fun decodeJson5Element(): Json5Element {
        val event = parser.peek()
        return when (event.item) {
            is Token.Value -> decodePrimitive()
            is Token.BeginObject -> decodeObject()
            is Token.BeginArray -> decodeArray()
            is Token.EndOfFile -> Json5Null
            else -> throw UnexpectedValueError("Unexpected token: ${event.item}", event.pos)
        }
    }

    private fun decodeObject(): Json5Object {
        val event = parser.next()
        if (event.item != Token.BeginObject) throw UnexpectedValueError("Expecting an object", event.pos)
        val map = mutableMapOf<String, Json5Element>()
        var memberName: String? = null
        while(true) {
            when(val token = parser.peek().item) {
                is Token.MemberName -> {
                    require(memberName == null) { "Duplicate member name" }
                    memberName = token.name
                }
                is Token.Value -> {
                    requireNotNull(memberName) { "Member must have a name" }
                    map[memberName] = decodePrimitive()
                    memberName = null
                }
                Token.BeginObject -> {
                    requireNotNull(memberName) { "Member must have a name" }
                    map[memberName] = decodeObject()
                    memberName = null
                }
                Token.BeginArray -> {
                    requireNotNull(memberName) { "Member must have a name" }
                    map[memberName] = decodeArray()
                    memberName = null
                }
                is Token.EndObject -> break
                else -> error("Unexpected token $token")
            }
            parser.next()
        }

        return Json5Object(map)
    }

    private fun decodePrimitive(): Json5Primitive {
        return when (val token = parser.peek().mapType<Token.Value>().item) {
            is Token.Num -> Json5Literal(token.rep, if ('.' in token.rep) Json5Primitive.Type.FLOAT else Json5Primitive.Type.INTEGER)
            is Token.Bool -> Json5Primitive(token.bool)
            is Token.Str -> Json5Primitive(token.string)
            Token.Null -> Json5Null
        }
    }

    private fun decodeArray(): Json5Array {
        val output = mutableListOf<Json5Element>()
        if(parser.next().item != Token.BeginArray) error("Not an array")
        while(true) {
            when(val token = parser.peek().item) {
                is Token.Value -> output.add(decodePrimitive())
                Token.BeginObject -> output.add(decodeObject())
                Token.BeginArray -> output.add(decodeArray())
                Token.EndArray -> break
                else -> error("Unexpected token $token")
            }
            parser.next()
        }
        return Json5Array(output)
    }
}

@ExperimentalSerializationApi
private class UnsignedDecoder(private val parent: MainDecoder) : Decoder, Json5Decoder by parent {
    override val serializersModule: SerializersModule = parent.serializersModule
    private val parser: LookaheadParser<Token> = parent.parser

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = throw UnsupportedOperationException()
    override fun decodeBoolean(): Boolean = throw UnsupportedOperationException()
    override fun decodeChar(): Char = throw UnsupportedOperationException()
    override fun decodeDouble(): Double = throw UnsupportedOperationException()
    override fun decodeFloat(): Float = throw UnsupportedOperationException()
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = throw UnsupportedOperationException()
    override fun decodeInline(descriptor: SerialDescriptor): Decoder = throw UnsupportedOperationException()
    override fun decodeString(): String = throw UnsupportedOperationException()

    override fun decodeByte(): Byte = parser.getUnsignedInteger(UnsignedLimits.BYTE).toByte()
    override fun decodeShort(): Short = parser.getUnsignedInteger(UnsignedLimits.SHORT).toShort()
    override fun decodeInt(): Int = parser.getUnsignedInteger(UnsignedLimits.INT).toInt()
    override fun decodeLong(): Long = parser.getUnsignedInteger(UnsignedLimits.LONG).toLong()

    override fun decodeNotNullMark(): Boolean = parent.decodeNotNullMark()
    override fun decodeNull(): Nothing? = parent.decodeNull()
}

private object SignedLimits {
    val BYTE = Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()
    val SHORT = Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()
    val INT = Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
    val LONG = Long.MIN_VALUE..Long.MAX_VALUE
}

private object UnsignedLimits {
    val BYTE = UByte.MAX_VALUE.toULong()
    val SHORT = UShort.MAX_VALUE.toULong()
    val INT = UInt.MAX_VALUE.toULong()
    val LONG = ULong.MAX_VALUE
}

private fun Token.Num.isHexNumber() = rep.startsWith("0x") || rep.startsWith("-0x")
private fun Token.Num.removeHexPrefix() = rep.replaceFirst("0x", "")

private fun Parser<Token>.getInteger(limits: LongRange): Long {
    val (pos, token) = next().mapType<Token.Num>()
    val num = if (token.isHexNumber()) {
        val hexString = token.removeHexPrefix()
        hexString.toLongOrNull(16)
    } else {
        token.rep.toLongOrNull()
    }

    if (num == null || num !in limits) {
        throw UnexpectedValueError("signed integer in range [$limits] expected", pos)
    }

    return num
}

private fun Parser<Token>.getUnsignedInteger(max: ULong): ULong {
    val (pos, token) = next().mapType<Token.Num>()
    val num = if (token.isHexNumber()) {
        val hexString = token.removeHexPrefix()
        hexString.toULongOrNull(16)
    } else {
        token.rep.toULongOrNull()
    }

    if (num == null || num > max) {
        throw UnexpectedValueError("unsigned integer in range [0..$max] expected", pos)
    }

    return num
}

private fun Parser<Token>.getDouble(): Double {
    val (pos, token) = next().mapType<Token.Num>()
    return token.rep.toDoubleOrNull() ?: throw UnexpectedValueError("floating-point number expected", pos)
}
