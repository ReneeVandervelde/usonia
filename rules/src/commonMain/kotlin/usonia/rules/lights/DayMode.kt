package usonia.rules.lights

import inkapplications.spondee.scalar.percent
import kotlinx.datetime.Clock
import usonia.foundation.Room
import usonia.foundation.unit.compareTo
import usonia.kotlin.first
import usonia.weather.WeatherAccess
import kotlin.time.ExperimentalTime
import kotlin.time.hours

/**
 * Keeps lights off in the daytime.
 */
@OptIn(ExperimentalTime::class)
internal class DayMode(
    private val weatherAccess: WeatherAccess,
    private val clock: Clock = Clock.System,
): LightSettingsPicker {
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
        val forecast = weatherAccess.forecast.first()
        val conditions = weatherAccess.conditions.first()
        return when {
            conditions.cloudCover > 60.percent -> LightSettings.Unhandled
            forecast.rainChance > 10.percent -> LightSettings.Unhandled
            forecast.snowChance > 10.percent -> LightSettings.Unhandled
            clock.now() < forecast.sunrise + 1.hours -> LightSettings.Unhandled
            clock.now() > forecast.sunset - 1.hours -> LightSettings.Unhandled
            else -> LightSettings.Ignore
        }
    }
}
