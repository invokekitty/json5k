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
    private var hexConfig: Hexadecimal? = null

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
            StructureKind.CLASS, StructureKind.OBJECT, StructureKind.MAP, is PolymorphicKind ->
                MutableJson5Object()
            StructureKind.LIST -> MutableJson5Array()
            else -> throw UnsupportedOperationException()
        }
        return this
    }

    override fun beginElement(descriptor: SerialDescriptor, index: Int) {
        hexConfig = descriptor.getElementAnnotations(index).firstNotNullOfOrNull { it as? Hexadecimal }
        elementStack.addLast(Json5Null)
    }

    override fun endElement(descriptor: SerialDescriptor, index: Int) {
        hexConfig = null
        val element = elementStack.removeLast()
        val name = descriptor.getElementName(index)
        val discriminator = descriptor.getClassDiscriminator(json5.settings)
        when (val outer = last) {
            is MutableJson5Object if (descriptor.kind is PolymorphicKind && name != discriminator) -> {
                for ((key, value) in element.asObject()) {
                    require(key != discriminator) { "member name '$key' is reserved" }
                    outer[key] = value
                    element.getComment(key)?.let { outer.putComment(key, it) }
                }
            }
            is MutableJson5Object if (descriptor.kind == StructureKind.MAP) -> when (val key = key) {
                null -> this.key = element as? Json5Primitive ?: throw SerializationException("Non-primitive keys are not allowed")
                else -> { outer[key.content] = element; this.key = null }
            }
            is MutableJson5Object -> {
                outer[name] = element
                val comment = descriptor.getElementAnnotations(index)
                    .filterIsInstance<SerialComment>().firstOrNull()
                comment?.let { outer.putComment(name, it.value) }
            }
            is MutableJson5Array -> outer.add(element)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (key != null) throw SerializationException("Incompletely encoded map")
    }

    override fun encodeJson5Element(element: Json5Element) {
        last = element
    }

    override fun encodeBoolean(value: Boolean) {
        last = Json5Primitive(value)
    }

    override fun encodeByte(value: Byte) {
        last = Json5Primitive(value, hexConfig)
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
        last = Json5Primitive(value, hexConfig)
    }

    override fun encodeLong(value: Long) {
        last = Json5Primitive(value, hexConfig)
    }

    override fun encodeNull() {
        last = Json5Null
    }

    override fun encodeShort(value: Short) {
        last = Json5Primitive(value, hexConfig)
    }

    override fun encodeString(value: String) {
        last = Json5Primitive(value)
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder =
        if (descriptor.isUnsignedNumber) this.UnsignedEncoder() else this
    
    private inner class UnsignedEncoder : Encoder {
        override fun encodeByte(value: Byte) {
            last = Json5Primitive(value.toUByte(), hexConfig)
        }

        override fun encodeInt(value: Int) {
            last = Json5Primitive(value.toUInt(), hexConfig)
        }

        override fun encodeLong(value: Long) {
            last = Json5Primitive(value.toULong(), hexConfig)
        }

        override fun encodeShort(value: Short) {
            last = Json5Primitive(value.toUShort(), hexConfig)
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            throw SerializationException()
        }

        override fun encodeBoolean(value: Boolean) {
            throw SerializationException()
        }

        override fun encodeChar(value: Char) {
            throw SerializationException()
        }

        override fun encodeDouble(value: Double) {
            throw SerializationException()
        }

        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
            throw SerializationException()
        }

        override fun encodeFloat(value: Float) {
            throw SerializationException()
        }

        override fun encodeInline(descriptor: SerialDescriptor): Encoder {
            throw SerializationException()
        }

        @ExperimentalSerializationApi
        override fun encodeNull() {
            throw SerializationException()
        }

        override fun encodeString(value: String) {
            throw SerializationException()
        }

        override val serializersModule: SerializersModule
            get() = json5.serializersModule
    }
}