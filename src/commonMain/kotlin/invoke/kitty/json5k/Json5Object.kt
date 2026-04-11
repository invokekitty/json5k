package invoke.kitty.json5k

import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable(with = Json5ObjectSerializer::class)
open class Json5Object(internal open val content: Map<String, Json5Element>) : Json5Element, Map<String, Json5Element> by content {

    internal open val comments: Map<String, String> get() = emptyMap()

    fun getComment(key: String): String? = comments[key]

    override fun equals(other: Any?): Boolean = content == other
    override fun hashCode(): Int = content.hashCode()
    override fun toString(): String {
        return content.entries.joinToString(
            separator = ",",
            prefix = "{",
            postfix = "}",
            transform = { (k, v) ->
                buildString {
                    append(k)
                    append(':')
                    append(v)
                }
            }
        )
    }

    internal class Commented(content: Map<String, Json5Element>, override val comments: Map<String, String>) : Json5Object(content)
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class MutableJson5Object(override val content: MutableMap<String, Json5Element>) : Json5Object(content), MutableMap<String, Json5Element> by content {
    override val comments: MutableMap<String, String> = mutableMapOf()

    constructor() : this(mutableMapOf())

    constructor(from: Json5Object) : this(from.content.toMutableMap())

    fun putComment(key: String, value: String) {
        comments[key] = value
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildJson5Object(builder: MutableJson5Object.() -> Unit): Json5Object {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    val obj = MutableJson5Object()
    obj.builder()
    return obj.toImmutable()
}

fun MutableJson5Object.toImmutable() =
    if (comments.isEmpty()) Json5Object(content) else Json5Object.Commented(content, comments)

fun Json5Object.toMutable() =
    MutableJson5Object(this).apply { comments += this.comments }