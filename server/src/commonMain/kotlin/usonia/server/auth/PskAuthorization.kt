package usonia.server.auth

import kimchi.logger.KimchiLogger
import kotlinx.datetime.Instant
import usonia.auth.Auth
import usonia.core.state.findBridgeAuthById
import usonia.foundation.Identifier
import usonia.server.client.BackendClient
import usonia.server.http.HttpRequest
import usonia.server.http.SocketCall

internal class PskAuthorization(
    private val client: BackendClient,
    private val authTracker: AuthTracker,
    private val logger: KimchiLogger,
): Authorization {
    override suspend fun validate(call: SocketCall): AuthResult {
        val signature = call.parameters[Auth.Signature.HEADER]
            ?.singleOrNull()
            ?.let { Auth.Signature(it) }
            ?: return AuthResult.Failure.IllegalSignature.also {
                logger.trace("Rejecting Request with no signature")
            }
        val timestamp = call.parameters[Auth.Timestamp.HEADER]
            ?.singleOrNull()
            ?.toLongOrNull()
            ?.let { Instant.fromEpochMilliseconds(it) }
            ?.let { Auth.Timestamp(it) }
            ?: return AuthResult.Failure.IllegalTimestamp.also {
                logger.trace("Rejecting Request with no timestamp")
            }
        val bridge = call.parameters[Auth.Bridge.HEADER]
            ?.singleOrNull()
            ?.let(::Identifier)
            ?.let { Auth.Bridge(it) }
            ?: return AuthResult.Failure.IllegalBridgeId.also {
                logger.trace("Rejecting Request with no ID")
            }
        val nonce = call.parameters[Auth.Nonce.HEADER]
            ?.singleOrNull()
            ?.let { Auth.Nonce(it) }
            ?: return AuthResult.Failure.IllegalNonce.also {
                logger.trace("Rejecting Request with no nonce")
            }
        val token = AuthParamToken(
            timestamp = timestamp,
            nonce = nonce,
        )
        val bridgePsk = client.findBridgeAuthById(bridge.id)
            ?.psk
            ?.let { Auth.Psk(it) }
            ?: return AuthResult.Failure.UnauthorizedBridge.also {
                logger.trace("Rejecting Request with no bridge config")
            }
        val expectedAuth = Auth.createSignature(
            body = null,
            timestamp = timestamp,
            psk = bridgePsk,
            nonce = nonce,
        )

        return consumeAndGetResult(token, signature, expectedAuth)
    }

    override suspend fun validate(request: HttpRequest): AuthResult
    {
        val signature = request.headers[Auth.Signature.HEADER]
            ?.singleOrNull()
            ?.let { Auth.Signature(it) }
            ?: return AuthResult.Failure.IllegalSignature.also {
                logger.trace("Rejecting Request with no signature")
            }
        val timestamp = request.headers[Auth.Timestamp.HEADER]
            ?.singleOrNull()
            ?.toLongOrNull()
            ?.let { Instant.fromEpochMilliseconds(it) }
            ?.let { Auth.Timestamp(it) }
            ?: return AuthResult.Failure.IllegalTimestamp.also {
                logger.trace("Rejecting Request with no timestamp")
            }
        val bridge = request.headers[Auth.Bridge.HEADER]
            ?.singleOrNull()
            ?.let(::Identifier)
            ?.let { Auth.Bridge(it) }
            ?: return AuthResult.Failure.IllegalBridgeId.also {
                logger.trace("Rejecting Request with no ID")
            }
        val nonce = request.headers[Auth.Nonce.HEADER]
            ?.singleOrNull()
            ?.let { Auth.Nonce(it) }
            ?: return AuthResult.Failure.IllegalNonce.also {
                logger.trace("Rejecting Request with no nonce")
            }
        val token = AuthParamToken(
            timestamp = timestamp,
            nonce = nonce,
        )
        val bridgePsk = client.findBridgeAuthById(bridge.id)
            ?.psk
            ?.let { Auth.Psk(it) }
            ?: return AuthResult.Failure.UnauthorizedBridge.also {
                logger.trace("Rejecting Request with no bridge config")
            }
        val expectedAuth = Auth.createSignature(
            body = request.body,
            timestamp = timestamp,
            psk = bridgePsk,
            nonce = nonce,
        )

        return consumeAndGetResult(token, signature, expectedAuth)
    }

    private fun consumeAndGetResult(
        token: AuthParamToken,
        signature: Auth.Signature,
        expectedAuth: Auth.Signature,
    ): AuthResult {
        try {
            authTracker.consume(token)
        } catch (e: AuthTracker.StaleToken) {
            logger.trace("Auth is stale", e)
            return AuthResult.Failure.StaleAuth
        } catch (e: AuthTracker.AlreadyConsumed) {
            logger.trace("Auth already consumed", e)
            return AuthResult.Failure.AlreadyConsumed
        }

        if (signature != expectedAuth) {
            logger.trace("Rejecting Request with invalid auth. Expected <$expectedAuth> but got <$signature>")
            return AuthResult.Failure.InvalidAuthorization
        }

        return AuthResult.Success
    }
}

