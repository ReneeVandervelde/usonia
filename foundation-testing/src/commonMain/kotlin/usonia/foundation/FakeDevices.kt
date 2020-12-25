package usonia.foundation

object FakeDevices {
    val Switch = Device(
        id = Identifier("fake-switch"),
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

    val Motion = Device(
        id = Identifier("fake-motion"),
        name = "Fake Motion Sensor",
        capabilities = Capabilities(
            actions = setOf(),
            events = setOf(
                Event.Motion::class
            )
        )
    )

    val WaterSensor = Device(
        id = Identifier("fake-water-sensor"),
        name = "Fake Water Sensor",
        capabilities = Capabilities(
            actions = emptySet(),
            events = setOf(
                Event.Water::class
            )
        )
    )

    val HueGroup = Device(
        id = Identifier("fake-hue-group"),
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

    val FakeHueBridge = Bridge(
        id = Identifier("fake-id"),
        name = "Fake Hue Bridge",
        service = "hue",
        parameters = mapOf(
            "baseUrl" to "fake-url",
            "token" to "fake-token",
        ),
    )
}
