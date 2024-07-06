package usonia.glass

import com.inkapplications.glassconsole.client.pin.NonceGenerator
import com.inkapplications.glassconsole.structures.pin.Nonce
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * Tracks open challenge nonces.
 *
 * These nonces are allowed to disarm the system, and are cleared after a
 * successful disarm. Only open nonces should be allowed to prevent replay
 * attacks.
 */
internal class ChallengeContainer(
    private val nonceGenerator: NonceGenerator,
) {
    private val open = MutableStateFlow<List<Nonce>>(emptyList())

    fun issue(): Nonce {
        val nonce = nonceGenerator.generateNonce()

        open.getAndUpdate { it + nonce }

        return nonce
    }

    fun consume(challenge: Nonce) {
        open.getAndUpdate { currentList ->
            when {
                challenge in currentList -> currentList - challenge
                else -> throw IllegalArgumentException("Invalid Nonce")
            }
        }
    }

    fun clear() {
        open.value = emptyList()
    }
}
