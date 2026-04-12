package invoke.kitty.json5k

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal object Json5ElementSerializer : KSerializer<Json5Element> {
    override val descriptor: SerialDescriptor = buildSerialDescriptor("invoke.kitty.json5k.Json5Element", SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): Json5Element {
        decoder as Json5Decoder
        return decoder.decodeJson5Element()
    }


    override fun serialize(encoder: Encoder, value: Json5Element) {
        encoder as Json5Encoder
        encoder.encodeJson5Element(value)
    }
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal object Json5ObjectSerializer : KSerializer<Json5Object> {

    override val descriptor: SerialDescriptor = buildSerialDescriptor("invoke.kitty.json5k.Json5Object", SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): Json5Object {
        decoder as Json5Decoder
        return decoder.decodeJson5Element() as? Json5Object ?: throw SerializationException("Not a Json5 object")
    }

    override fun serialize(encoder: Encoder, value: Json5Object) {
        encoder as Json5Encoder
        encoder.encodeJson5Element(value)
    }
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal object Json5ArraySerializer : KSerializer<Json5Array> {

    override val descriptor: SerialDescriptor = buildSerialDescriptor("invoke.kitty.json5k.Json5Array", SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): Json5Array {
        decoder as Json5Decoder
        return decoder.decodeJson5Element() as? Json5Array ?: throw SerializationException("Not a Json5 array")
    }

    override fun serialize(encoder: Encoder, value: Json5Array) {
        encoder as Json5Encoder
        encoder.encodeJson5Element(value)
    }
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal object Json5PrimitiveSerializer : KSerializer<Json5Primitive> {

    override val descriptor: SerialDescriptor = buildSerialDescriptor("invoke.kitty.json5k.Json5Primitive", SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): Json5Primitive {
        decoder as Json5Decoder
        return decoder.decodeJson5Element() as? Json5Primitive ?: throw SerializationException("Not a Json5 primitive")
    }

    override fun serialize(encoder: Encoder, value: Json5Primitive) {
        encoder as Json5Encoder
        encoder.encodeJson5Element(value)
    }
}