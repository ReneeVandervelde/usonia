package usonia.foundation

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

    val Dry = Event.Water(
        source = FakeDevices.WaterSensor.id,
        timestamp = Instant.fromEpochSeconds(1),
        state = WaterState.DRY
    )
}
