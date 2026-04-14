package invoke.kitty.json5k

import kotlin.experimental.and
import kotlin.math.abs

internal fun String.isHexNumber() = startsWith("0x") || startsWith("-0x")

internal fun String.removeHexPrefix() = replaceFirst("0x", "")

internal fun String.parseLong(bounds: LongRange): Result<Long> {
    val result = runCatching { if(isHexNumber()) removeHexPrefix().toLong(16) else toLong() }
    return result.mapCatching { require(it in bounds); it }
}

internal fun String.parseULong(bounds: ULongRange): Result<ULong> {
    val result = runCatching { if (isHexNumber()) removeHexPrefix().toULong(16) else toULong() }
    return result.mapCatching { require(it in bounds); it }
}

internal fun Byte.toSignedHexString(format: HexFormat): String {
    if (this >= 0) return toHexString(format)
    return "-" + abs(toInt()).toUByte().toHexString(format)
}

internal fun Short.toSignedHexString(format: HexFormat): String {
    if (this >= 0) return toHexString(format)
    return "-" + abs(toInt()).toUShort().toHexString(format)
}

internal fun Int.toSignedHexString(format: HexFormat): String {
    if (this >= 0) return toHexString(format)
    return "-" + abs(this).toUInt().toHexString(format)
}

internal fun Long.toSignedHexString(format: HexFormat): String {
    if (this >= 0) return toHexString(format)
    return "-" + abs(this).toULong().toHexString(format)
}