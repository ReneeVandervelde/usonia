package usonia.foundation

import inkapplications.spondee.scalar.percent
import kotlinx.datetime.Instant

object FakeEvents {
    val SwitchOn = Event.Switch(
        source = FakeDevices.Switch.id,
        timestamp = Instant.fromEpochSeconds(1),
        state = SwitchState.ON
    )

    val SwitchOff = Event.Switch(
        source = FakeDevices.Switch.id,
        timestamp = Instant.fromEpochSeconds(2),
        state = SwitchState.OFF
    )

    val Wet = Event.Water(
        source = FakeDevices.WaterSensor.id,
        timestamp = Instant.fromEpochSeconds(1),
        state = WaterState.WET
    )

    val LowBattery = Event.Battery(
        source = FakeDevices.WaterSensor.id,
        timestamp = Instant.fromEpochSeconds(1),
        percentage = 1.percent,
    )

    val FullBattery = Event.Battery(
        source = FakeDevices.WaterSensor.id,
        timestamp = Instant.fromEpochSeconds(1),
        percentage = 100.percent,
    )

    val Away = Event.Presence(
        source = FakeUsers.John.id,
        timestamp = Instant.fromEpochSeconds(1),
        state = PresenceState.AWAY
    )

    val Dry = Event.Water(
        source = FakeDevices.WaterSensor.id,
        timestamp = Instant.fromEpochSeconds(1),
        state = WaterState.DRY
    )
}
