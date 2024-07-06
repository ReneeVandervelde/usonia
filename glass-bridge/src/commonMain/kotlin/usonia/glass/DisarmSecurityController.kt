package usonia.glass

import com.inkapplications.glassconsole.client.pin.PinValidator
import com.inkapplications.glassconsole.structures.pin.ChallengeResponse
import com.inkapplications.glassconsole.structures.pin.Pin
import com.inkapplications.glassconsole.structures.pin.Psk
import kimchi.logger.KimchiLogger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import usonia.core.state.findBridgeById
import usonia.foundation.Identifier
import usonia.foundation.Status
import usonia.foundation.Statuses
import usonia.server.client.BackendClient
import usonia.server.http.HttpRequest
import usonia.server.http.RestController
import usonia.server.http.RestResponse

/**
 * Disarms the site security system only if a valid pin challenge is provided.
 */
internal class DisarmSecurityController(
    private val backendClient: BackendClient,
    private val pinValidator: PinValidator,
    private val challengeContainer: ChallengeContainer,
    json: Json,
    logger: KimchiLogger,
): RestController<ChallengeResponse, Status>(json, logger) {
    override val path: String = "/glass/{bridgeId}/disarm"
    override val serializer: KSerializer<Status> = Status.serializer()
    override val deserializer: KSerializer<ChallengeResponse> = ChallengeResponse.serializer()
    override val method: String = "POST"

    override suspend fun getResponse(data: ChallengeResponse, request: HttpRequest): RestResponse<Status> {
        val bridgeId = request.parameters["bridgeId"]
            ?.firstOrNull()
            ?.let(::Identifier)
            ?: return RestResponse(
                status = 400,
                data = Statuses.missingRequired("bridgeId")
            )
        val bridge = backendClient.findBridgeById(bridgeId) ?: return RestResponse(
            status = 404,
            data = Statuses.BRIDGE_NOT_FOUND
        )
        val pin = bridge.parameters["pin"]?.let(::Pin) ?: return RestResponse(
            status = 500,
            data = Statuses.bridgeNotConfigured(bridgeId)
        )
        val psk = bridge.parameters["psk"]?.let(::Psk) ?: return RestResponse(
            status = 500,
            data = Statuses.bridgeNotConfigured(bridgeId)
        )
        val reconstruction = pinValidator.digest(
            psk = psk,
            pin = pin,
            timestamp = data.timestamp,
            nonce = data.nonce,
        )

        try {
            challengeContainer.consume(data.nonce)
        } catch (e: IllegalArgumentException) {
            return RestResponse(
                status = 400,
                data = Statuses.illegalArgument("Invalid Nonce")
            )
        }

        if (reconstruction.digest != data.digest) {
            return RestResponse(
                status = 401,
                data = Statuses.illegalArgument("Challenge Invalid")
            )
        }

        logger.info("Pin Challenge Validated. Disarming Security.")
        challengeContainer.clear()
        backendClient.disarmSecurity()

        return RestResponse(data = Statuses.SUCCESS)
    }
}
