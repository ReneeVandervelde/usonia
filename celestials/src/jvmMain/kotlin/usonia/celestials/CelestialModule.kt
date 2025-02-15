package usonia.celestials

import com.inkapplications.datetime.ZonedClock
import usonia.server.client.BackendClient

class CelestialModule(
    usoniaClient: BackendClient,
    clock: ZonedClock,
) {
    val celestialAccess: CelestialAccess = JvmCelestialAccess(
        usonia = usoniaClient,
        clock = clock,
    )
}
