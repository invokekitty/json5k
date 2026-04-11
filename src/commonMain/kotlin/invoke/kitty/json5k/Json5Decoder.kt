package invoke.kitty.json5k

interface Json5Decoder {

    val json5: Json5

    fun decodeJson5Element(): Json5Element

}