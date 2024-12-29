package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.collectLatest
import com.inkapplications.coroutines.ongoing.combinePair
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.processes.daemon.Daemon
import usonia.core.state.publishAll
import usonia.foundation.*
import usonia.server.client.BackendClient

class LightsOffOnSecurityArm(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun startDaemon(): Nothing {
        client.securityState
            .combinePair(client.site)
            .collectLatest { (state, site) ->
            when (state) {
                SecurityState.Armed -> site
                    .findDevicesBy {
                        it.fixture == Fixture.Light && Action.Switch::class in it.capabilities.actions
                    }
                    .map {
                        Action.Switch(it.id, SwitchState.ON)
                    }
                    .run {
                        logger.info("Turning off ${size} lights for security arm.")
                        client.publishAll(this)
                    }
                SecurityState.Disarmed -> {}
            }
        }
    }
}
