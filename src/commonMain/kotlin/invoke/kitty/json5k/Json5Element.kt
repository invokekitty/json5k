@file:OptIn(ExperimentalContracts::class)

package invoke.kitty.json5k

import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Serializable(with = Json5ElementSerializer::class)
sealed interface Json5Element

fun Json5Element.asPrimitive(): Json5Primitive {
    contract {
        returns() implies (this@asPrimitive is Json5Primitive)
    }
    return this as? Json5Primitive ?: throw IllegalArgumentException("$this is not a Json5Primitive")
}

fun Json5Element.asObject(): Json5Object {
    contract {
        returns() implies (this@asObject is Json5Object)
    }
    return this as? Json5Object ?: throw IllegalArgumentException("$this is not a Json5Object")
}

fun Json5Element.asArray(): Json5Array {
    contract {
        returns() implies (this@asArray is Json5Array)
    }
    return this as? Json5Array ?: throw IllegalArgumentException("$this is not a Json5Array")
}

