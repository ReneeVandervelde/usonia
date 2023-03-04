package usonia.foundation

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A Universally unique Identifier String.
 */
@Serializable
@JvmInline
value class Identifier(val value: String) {
    override fun toString(): String {
        return value
    }
}
