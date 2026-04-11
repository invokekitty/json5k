package invoke.kitty.json5k.serialization

import invoke.kitty.json5k.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
class Json5ElementEncoder(
    override val json5: Json5
) : AbstractEncoder(), Json5Encoder {
    override val serializersModule: SerializersModule
        get() = json5.serializersModule

    private val elementStack: ArrayDeque<Json5Element> = ArrayDeque()
    private var key: Json5Primitive? = null

    private var last: Json5Element
        get() = elementStack.last()
        set(value) {
            elementStack[elementStack.lastIndex] = value
        }

    init {
        elementStack.addLast(Json5Null)
    }

    internal fun get(): Json5Element {
        if(elementStack.size > 1) throw SerializationException("Some element is still open")
        return last
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        last = when(descriptor.kind) {
            StructureKind.CLASS, StructureKind.OBJECT, is PolymorphicKind -> MutableJson5Object()
            StructureKind.MAP -> MutableJson5ObjectForMapEncoding()
            StructureKind.LIST -> MutableJson5Array()
            else -> throw UnsupportedOperationException()
        }
        return this
    }

    override fun beginElement(descriptor: SerialDescriptor, index: Int) {
        elementStack.addLast(Json5Null)
    }

    override fun endElement(descriptor: SerialDescriptor, index: Int) {
        val element = elementStack.removeLast()
        when (val outer = last) {
            is MutableJson5Object -> {
                val comment = descriptor.getElementAnnotations(index)
                    .find { it is SerialComment } as SerialComment?
                val key = descriptor.getElementName(index)
                outer[key] = element
                comment?.let { outer.putComment(key, it.value) }
            }
            is MutableJson5Array -> outer.add(element)
            is MutableJson5ObjectForMapEncoding -> when (val key = key) {
                null -> this.key = element as? Json5Primitive ?: throw SerializationException("Non-primitive keys are not allowed")
                else -> { outer[key.content] = element; this.key = null }
            }
            else -> throw UnsupportedOperationException()
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (key != null) throw SerializationException("Incompletely encoded map")
        last = last.toImmutable()
    }

    override fun encodeJson5Element(element: Json5Element) {
        last = element.toImmutable()
    }

    override fun encodeBoolean(value: Boolean) {
        last = Json5Primitive(value)
    }

    override fun encodeByte(value: Byte) {
        last = Json5Primitive(value)
    }

    override fun encodeChar(value: Char) {
        last = Json5Primitive(value.toString())
    }

    override fun encodeDouble(value: Double) {
        last = Json5Primitive(value)
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        last = Json5Primitive(enumDescriptor.getElementName(index))
    }

    override fun encodeFloat(value: Float) {
        last = Json5Primitive(value)
    }

    override fun encodeInt(value: Int) {
        last = Json5Primitive(value)
    }

    override fun encodeLong(value: Long) {
        last = Json5Primitive(value)
    }

    override fun encodeNull() {
        last = Json5Null
    }

    override fun encodeShort(value: Short) {
        last = Json5Primitive(value)
    }

    override fun encodeString(value: String) {
        last = Json5Primitive(value)
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

    private fun Json5Element.toImmutable(): Json5Element = when (this) {
        is MutableJson5Object, is MutableJson5ObjectForMapEncoding ->
            if (comments.isNotEmpty()) Json5Object.Commented(this.content, this.comments) else Json5Object(this.content)
        is MutableJson5Array -> Json5Array(this.content)
        else -> this
    }

    @Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
    private class MutableJson5ObjectForMapEncoding(content: MutableMap<String, Json5Element> = mutableMapOf()) : Json5Object(content), MutableMap<String, Json5Element> by content
}