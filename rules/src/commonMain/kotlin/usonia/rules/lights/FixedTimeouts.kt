package usonia.rules.lights

import usonia.foundation.Room
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

/**
 * Hardcodes a fixed time for rooms to idle based on their type.
 */
@OptIn(ExperimentalTime::class)
internal object FixedTimeouts: LightSettingsPicker {
    override suspend fun getIdleConditions(room: Room): IdleConditions {
        return when(room.type) {
            Room.Type.Bathroom -> 5.minutes
            Room.Type.Bedroom -> 5.minutes
            Room.Type.Dining -> 10.minutes
            Room.Type.Garage -> 15.minutes
            Room.Type.Generic -> 10.minutes
            Room.Type.Hallway -> 5.seconds
            Room.Type.Kitchen -> 10.minutes
            Room.Type.LivingRoom -> 30.minutes
            Room.Type.Office -> 15.minutes
            Room.Type.Storage -> 1.minutes
            Room.Type.Utility -> 1.minutes
            Room.Type.Greenhouse -> return IdleConditions.Unhandled
        }.let(IdleConditions::Timed)
    }
}
