package usonia.foundation

object FakeDevices {
    val Switch = Device(
        id = Uuid("fake-switch"),
        name = "Fake Switch",
        capabilities = Capabilities(
            actions = setOf(
                Action.Switch::class
            ),
            events = setOf(
                Event.Switch::class
            )
        )
    )

    val WaterSensor = Device(
        id = Uuid("fake-water-sensor"),
        name = "Fake Water Sensor",
        capabilities = Capabilities(
            actions = emptySet(),
            events = setOf(
                Event.Water::class
            )
        )
    )
}
