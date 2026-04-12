@file:Suppress("UNUSED")

package invoke.kitty.json5k

import kotlinx.serialization.Serializable

@Serializable(with = Json5PrimitiveSerializer::class)
sealed class Json5Primitive(val content: String, val type: Type) : Json5Element {

    override fun toString(): String = if(type == Type.STRING) "\"$content\"" else content

    enum class Type {
        STRING,
        BOOLEAN,
        INTEGER,
        FLOAT,
        NULL
    }

}

fun Json5Primitive(value: String): Json5Primitive = Json5Literal(value, Json5Primitive.Type.STRING)

fun Json5Primitive(value: Number): Json5Primitive {
    return if (value is Float || value is Double) Json5Primitive(value.toDouble())
    else Json5Primitive(value.toLong())
}

fun Json5Primitive(value: Byte, hex: Hexadecimal? = null): Json5Primitive =
    Json5Literal(hex?.let { value.toHexString(it.toHexFormat()) } ?: value.toString(), Json5Primitive.Type.INTEGER)

fun Json5Primitive(value: UByte, hex: Hexadecimal? = null): Json5Primitive =
    Json5Literal(hex?.let { value.toHexString(it.toHexFormat()) } ?: value.toString(), Json5Primitive.Type.INTEGER)

fun Json5Primitive(value: Short, hex: Hexadecimal? = null): Json5Primitive =
    Json5Literal(hex?.let { value.toHexString(it.toHexFormat()) } ?: value.toString(), Json5Primitive.Type.INTEGER)

fun Json5Primitive(value: UShort, hex: Hexadecimal? = null): Json5Primitive =
    Json5Literal(hex?.let { value.toHexString(it.toHexFormat()) } ?: value.toString(), Json5Primitive.Type.INTEGER)

fun Json5Primitive(value: Int, hex: Hexadecimal? = null): Json5Primitive =
    Json5Literal(hex?.let { value.toHexString(it.toHexFormat()) } ?: value.toString(), Json5Primitive.Type.INTEGER)

fun Json5Primitive(value: UInt, hex: Hexadecimal? = null): Json5Primitive =
    Json5Literal(hex?.let { value.toHexString(it.toHexFormat()) } ?: value.toString(), Json5Primitive.Type.INTEGER)

fun Json5Primitive(value: Long, hex: Hexadecimal? = null): Json5Primitive =
    Json5Literal(hex?.let { value.toHexString(it.toHexFormat()) } ?: value.toString(), Json5Primitive.Type.INTEGER)

fun Json5Primitive(value: ULong, hex: Hexadecimal? = null): Json5Primitive =
    Json5Literal(hex?.let { value.toHexString(it.toHexFormat()) } ?: value.toString(), Json5Primitive.Type.INTEGER)

fun Json5Primitive(value: Float): Json5Primitive = Json5Literal(value.toString(), Json5Primitive.Type.FLOAT)

fun Json5Primitive(value: Double): Json5Primitive = Json5Literal(value.toString(), Json5Primitive.Type.FLOAT)

fun Json5Primitive(value: Boolean): Json5Primitive =
    Json5Literal(if (value) "true" else "false", Json5Primitive.Type.BOOLEAN)

@Suppress("FunctionName")
fun Json5Primitive(value: Nothing?): Json5Null = Json5Null

object Json5Null : Json5Primitive("null", Type.NULL)

class Json5Literal internal constructor(content: String, type: Type) : Json5Primitive(content, type) {
    init {
        require(type != Type.NULL) { "Json5Literal cannot be of type NULL" }
    }
}

val Json5Primitive.byteOrNull: Byte?
    get() = content.parseLong(Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()).getOrNull()?.toByte()

val Json5Primitive.byte: Byte
    get() = content.parseLong(Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()).getOrThrow().toByte()

val Json5Primitive.shortOrNull: Short?
    get() = content.parseLong(Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()).getOrNull()?.toShort()

val Json5Primitive.short: Short
    get() = content.parseLong(Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()).getOrThrow().toShort()

val Json5Primitive.intOrNull: Int?
    get() = content.parseLong(Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()).getOrNull()?.toInt()

val Json5Primitive.int: Int
    get() = content.parseLong(Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()).getOrThrow().toInt()

val Json5Primitive.longOrNull: Long?
    get() = content.parseLong(Long.MIN_VALUE..Long.MAX_VALUE).getOrNull()

val Json5Primitive.long: Long
    get() = content.parseLong(Long.MIN_VALUE..Long.MAX_VALUE).getOrThrow()

val Json5Primitive.ubyteOrNull: UByte?
    get() = content.parseULong(UByte.MIN_VALUE.toULong()..UByte.MAX_VALUE.toULong()).getOrNull()?.toUByte()

val Json5Primitive.ubyte: UByte
    get() = content.parseULong(UByte.MIN_VALUE.toULong()..UByte.MAX_VALUE.toULong()).getOrThrow().toUByte()

val Json5Primitive.ushortOrNull: UShort?
    get() = content.parseULong(UShort.MIN_VALUE.toULong()..UShort.MAX_VALUE.toULong()).getOrNull()?.toUShort()

val Json5Primitive.ushort: UShort
    get() = content.parseULong(UShort.MIN_VALUE.toULong()..UShort.MAX_VALUE.toULong()).getOrThrow().toUShort()

val Json5Primitive.uintOrNull: UInt?
    get() = content.parseULong(UInt.MIN_VALUE.toULong()..UInt.MAX_VALUE.toULong()).getOrNull()?.toUInt()

val Json5Primitive.uint: UInt
    get() = content.parseULong(UInt.MIN_VALUE.toULong()..UInt.MAX_VALUE.toULong()).getOrThrow().toUInt()

val Json5Primitive.ulongOrNull: ULong?
    get() = content.parseULong(ULong.MIN_VALUE..ULong.MAX_VALUE).getOrNull()

val Json5Primitive.ulong: ULong
    get() = content.parseULong(ULong.MIN_VALUE..ULong.MAX_VALUE).getOrThrow()

val Json5Primitive.floatOrNull: Float?
    get() = content.toFloatOrNull()

val Json5Primitive.float: Float
    get() = content.toFloat()

val Json5Primitive.doubleOrNull: Double?
    get() = content.toDoubleOrNull()

val Json5Primitive.double: Double
    get() = content.toDouble()

val Json5Primitive.boolean: Boolean
    get() = content.toBooleanStrict()

val Json5Primitive.booleanOrNull: Boolean?
    get() = content.toBooleanStrictOrNull()

val Json5Primitive.isString: Boolean
    get() = type == Json5Primitive.Type.STRING

val Json5Primitive.stringOrNull: String?
    get() = if(isString) content else null

val Json5Primitive.charOrNull: Char?
    get() = if(isString && content.length == 1) content[0] else null

val Json5Primitive.char: Char
    get() = charOrNull ?: error("element is not a char")