package usonia.server.auth

import com.ionspin.kotlin.crypto.hash.Hash
import com.ionspin.kotlin.crypto.util.encodeToUByteArray
import com.ionspin.kotlin.crypto.util.toHexString
import kimchi.logger.KimchiLogger
import kotlinx.datetime.Instant
import usonia.core.state.findBridgeAuthById
import usonia.foundation.Identifier
import usonia.server.client.BackendClient
import usonia.server.http.HttpRequest

internal class PskAuthorization(
    private val client: BackendClient,
    private val logger: KimchiLogger,
): Authorization {
    override suspend fun validate(request: HttpRequest): AuthResult
    {
        val auth = request.headers[HeaderKeys.Signature]
            ?.singleOrNull()
            ?: return AuthResult.Failure.IllegalSignature.also {
                logger.trace("Rejecting Request with no signature")
            }
        val timestamp = request.headers[HeaderKeys.Timestamp]
            ?.singleOrNull()
            ?.toLongOrNull()
            ?.let { Instant.fromEpochMilliseconds(it) }
            ?: return AuthResult.Failure.IllegalTimestamp.also {
                logger.trace("Rejecting Request with no timestamp")
            }
        val bridge = request.headers[HeaderKeys.BridgeId]
            ?.singleOrNull()
            ?.let(::Identifier)
            ?: return AuthResult.Failure.IllegalBridgeId.also {
                logger.trace("Rejecting Request with no ID")
            }
        val bridgePsk = client.findBridgeAuthById(bridge)
            ?.psk
            ?: return AuthResult.Failure.UnauthorizedBridge.also {
                logger.trace("Rejecting Request with no bridge config")
            }
        val expectedAuth = (request.body.orEmpty() + timestamp.toEpochMilliseconds().toString() + bridgePsk)
            .encodeToUByteArray()
            .let(Hash::sha256)
            .toHexString()

        if (auth != expectedAuth) {
            logger.trace("Rejecting Request with invalid auth. Expected <$expectedAuth> but got <$auth>")
            return AuthResult.Failure.InvalidAuthorization
        }

        return AuthResult.Success
    }

    object HeaderKeys
    {
        const val Signature = "X-Signature"
        const val Timestamp = "X-Timestamp"
        const val BridgeId = "X-Bridge-Id"
    }
}

