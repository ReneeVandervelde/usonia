package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.first
import com.inkapplications.datetime.ZonedClock
import inkapplications.spondee.scalar.percent
import usonia.celestials.CelestialAccess
import usonia.foundation.Room
import usonia.foundation.unit.compareTo
import usonia.weather.LocalWeatherAccess
import usonia.weather.getLatestConditions
import usonia.weather.getLatestForecast
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

/**
 * Keeps lights off in the daytime.
 */
@OptIn(ExperimentalTime::class)
internal class DayMode(
    private val weatherAccess: LocalWeatherAccess,
    private val celestialsAccess: CelestialAccess,
    private val clock: ZonedClock = ZonedClock.System,
): LightSettingsPicker {
    /**
     * Amount of time before/after sunrise/sunset to start turning lights on/off.
     */
    private val twilightBuffer = (1.5).hours

    override suspend fun getActiveSettings(room: Room): LightSettings {
        return when (room.type) {
            Room.Type.Bathroom,
            Room.Type.Hallway,
            Room.Type.Office,
            Room.Type.Utility,
            Room.Type.Storage,
            Room.Type.Garage,
            Room.Type.Greenhouse -> LightSettings.Unhandled
            Room.Type.Bedroom,
            Room.Type.Dining,
            Room.Type.Generic,
            Room.Type.Kitchen,
            Room.Type.LivingRoom -> handleRoom()
        }
    }

    private suspend fun handleRoom(): LightSettings {
        val forecast = weatherAccess.getLatestForecast()
        val conditions = weatherAccess.getLatestConditions()
        val celestials = celestialsAccess.localCelestials.first().today
        val currentCloudCover = conditions?.cloudCover
        val currentRainChance = forecast?.rainChance
        val currentSnowChance = forecast?.snowChance

        return when {
            currentCloudCover == null || currentCloudCover > 40.percent -> LightSettings.Unhandled
            currentRainChance != null && currentRainChance > 10.percent -> LightSettings.Unhandled
            currentSnowChance != null && currentSnowChance > 10.percent -> LightSettings.Unhandled
            clock.zonedDateTime() < celestials.daylight.start + twilightBuffer -> LightSettings.Unhandled
            clock.zonedDateTime() > celestials.daylight.endInclusive - twilightBuffer -> LightSettings.Unhandled
            else -> LightSettings.Ignore
        }
    }
}
