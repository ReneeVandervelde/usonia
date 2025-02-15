package usonia.rules.lights

import usonia.foundation.Room
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Hardcodes a fixed time for rooms to idle based on their type.
 */
internal object FixedTimeouts: LightSettingsPicker
{
    override suspend fun getIdleConditions(room: Room): IdleConditions
    {
        return when(room.type) {
            Room.Type.Bathroom -> IdleConditions.Timed(5.minutes)
            Room.Type.Bedroom -> IdleConditions.Timed(5.minutes)
            Room.Type.Dining -> IdleConditions.Timed(10.minutes)
            Room.Type.Garage -> IdleConditions.Timed(15.minutes)
            Room.Type.Generic -> IdleConditions.Timed(10.minutes)
            Room.Type.Hallway -> IdleConditions.Phased(
                startAfter = 5.seconds,
                endAfter = 2.minutes,
            )
            Room.Type.Kitchen -> IdleConditions.Phased(
                startAfter = 2.minutes,
                endAfter = 30.minutes,
            )
            Room.Type.LivingRoom -> IdleConditions.Timed(30.minutes)
            Room.Type.Office -> IdleConditions.Timed(15.minutes)
            Room.Type.Storage -> IdleConditions.Timed(1.minutes)
            Room.Type.Utility -> IdleConditions.Timed(1.minutes)
            Room.Type.Greenhouse -> IdleConditions.Unhandled
        }
    }
}
