package usonia.celestials

import com.inkapplications.coroutines.ongoing.OngoingFlow

/**
 * Provides access to solar event data.
 */
interface CelestialAccess
{
    /**
     * Get the latest schedule of solar events for the current site's location.
     */
    val localCelestials: OngoingFlow<UpcomingCelestials>
}
