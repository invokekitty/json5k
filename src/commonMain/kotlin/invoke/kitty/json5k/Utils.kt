package invoke.kitty.json5k

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