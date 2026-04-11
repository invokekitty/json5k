package invoke.kitty.json5k.deserialization

import invoke.kitty.json5k.UnknownKeyError
import invoke.kitty.json5k.format.Token
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule

internal class ObjectDecoder(parent: MainDecoder) : StructDecoder(parent, Token.BeginObject) {
    override val serializersModule: SerializersModule = parent.serializersModule

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val (pos, token) = parser.peek()

        if (token is Token.MemberName) {
            throw UnknownKeyError(token.name, pos)
        }

        return CompositeDecoder.DECODE_DONE
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        parser.next()
    }
}
