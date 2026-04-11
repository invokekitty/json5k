package invoke.kitty.json5k

import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable(with = Json5ArraySerializer::class)
open class Json5Array(internal open val content: List<Json5Element>) : Json5Element, List<Json5Element> by content {
    override fun equals(other: Any?): Boolean = content == other
    override fun hashCode(): Int = content.hashCode()
    override fun toString(): String = content.joinToString(prefix = "[", postfix = "]", separator = ",")
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class MutableJson5Array(override val content: MutableList<Json5Element>) : Json5Array(content), MutableList<Json5Element> by content {
    constructor() : this(mutableListOf())
    constructor(from: Json5Array) : this(from.content.toMutableList())
}

/**
 * Build a json 5 array
 */
@OptIn(ExperimentalContracts::class)
inline fun buildJson5Array(builder: MutableJson5Array.() -> Unit): Json5Array {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    val array = MutableJson5Array()
    array.builder()
    return array.toImmutable()
}

fun MutableJson5Array.toImmutable() = Json5Array(this)

fun Json5Array.toMutable() = MutableJson5Array(this)