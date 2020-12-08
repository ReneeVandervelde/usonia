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

    val HueGroup = Device(
        id = Uuid("fake-hue-group"),
        name = "Fake Hue Group",
        capabilities = Capabilities(
            actions = setOf(
                Action.Switch::class,
                Action.Dim::class,
                Action.ColorChange::class,
                Action.ColorTemperatureChange::class,
            ),
            events = emptySet()
        )
    )

    val FakeHueBridge = Bridge.Hue(
        id = Uuid("fake-id"),
        name = "Fake Hue Bridge",
        deviceMap = emptyMap(),
        baseUrl = "fake-url",
        token = "fake-token",
    )
}
