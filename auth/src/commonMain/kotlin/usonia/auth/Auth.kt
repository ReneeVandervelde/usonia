package usonia.auth

import com.ionspin.kotlin.crypto.hash.Hash
import com.ionspin.kotlin.crypto.util.encodeToUByteArray
import com.ionspin.kotlin.crypto.util.toHexString
import kotlinx.datetime.Instant
import usonia.foundation.Identifier
import kotlin.jvm.JvmInline

object Auth
{
    @JvmInline
    value class Signature(val value: String)
    {
        companion object
        {
            const val HEADER = "X-Signature"
        }
    }

    @JvmInline
    value class Timestamp(val instant: Instant)
    {
        companion object
        {
            const val HEADER = "X-Timestamp"
        }
    }

    @JvmInline
    value class Bridge(val id: Identifier)
    {
        companion object
        {
            const val HEADER = "X-Bridge-Id"
        }
    }

    @JvmInline
    value class Nonce(val value: String)
    {
        companion object
        {
            const val HEADER = "X-Nonce"
        }
    }

    @JvmInline
    value class Psk(val value: String)

    fun createSignature(
        body: String?,
        timestamp: Timestamp,
        psk: Psk,
        nonce: Nonce,
    ): Signature {
        val data = "${body.orEmpty()}${timestamp.instant.toEpochMilliseconds()}${psk.value}${nonce.value}"
        val hash = data.encodeToUByteArray()
            .let(Hash::sha256)
            .toHexString()

        return Signature(hash)
    }
}
