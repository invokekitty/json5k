package invoke.kitty.json5k

interface Json5Encoder {

    val json5: Json5

    fun encodeJson5Element(element: Json5Element)
}