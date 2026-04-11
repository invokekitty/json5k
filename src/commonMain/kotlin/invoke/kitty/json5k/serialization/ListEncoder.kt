package invoke.kitty.json5k.serialization

import invoke.kitty.json5k.format.Token
import kotlinx.serialization.descriptors.SerialDescriptor

internal class ListEncoder(parent: MainEncoder) : StructEncoder(parent) {
    init {
        generator.put(Token.BeginArray)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        generator.put(Token.EndArray)
    }
}
