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

    val Latch = Device(
        id = Identifier("fake-latch"),
        name = "Fake Latch",
        capabilities = Capabilities(
            actions = emptySet(),
            events = setOf(
                Event.Latch::class
            )
        )
    )

    val Lock = Device(
        id = Identifier("fake-lock"),
        name = "Fake Lock",
        capabilities = Capabilities(
            actions = setOf(
                Action.Lock::class
            ),
            events = setOf(
                Event.Lock::class
            ),
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

    val TemperatureSensor = Device(
        id = Identifier("fake-temperature-sensor"),
        name = "Fake Temperature Sensor",
        capabilities = Capabilities(
            actions = emptySet(),
            events = setOf(
                Event.Temperature::class
            )
        )
    )

    val HueGroup = Device(
        id = Identifier("fake-hue-group"),
        name = "Fake Hue Group",
        fixture = Fixture.Light,
        capabilities = Capabilities(
            archetypeId = "usonia.hue.group",
            actions = setOf(
                Action.Switch::class,
                Action.Dim::class,
                Action.ColorChange::class,
                Action.ColorTemperatureChange::class,
            ),
            events = emptySet()
        )
    )

    val HueColorLight = Device(
        id = Identifier("fake-hue-color-light"),
        name = "Fake Hue Color Light",
        fixture = Fixture.Light,
        capabilities = Capabilities(
            archetypeId = "usonia.hue.color",
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
