package usonia.smartthings

import usonia.foundation.Action
import usonia.foundation.Capabilities
import usonia.foundation.Event

object SmartThingsArchetypes {
    val motion = Capabilities(
        archetypeId = "usonia.smartthings.motion",
        actions = emptySet(),
        events = setOf(
            Event.Motion::class,
            Event.Temperature::class,
            Event.Battery::class,
        )
    )

    val humidity = Capabilities(
        archetypeId = "usonia.smartthings.humidity",
        actions = emptySet(),
        events = setOf(
            Event.Temperature::class,
            Event.Humidity::class,
        ),
    )

    val moisture = Capabilities(
        archetypeId = "usonia.smartthings.moisture",
        actions = emptySet(),
        events = setOf(
            Event.Temperature::class,
            Event.Water::class,
        ),
    )

    val multi = Capabilities(
        archetypeId = "usonia.smartthings.multi",
        actions = emptySet(),
        events = setOf(
            Event.Movement::class,
            Event.Tilt::class,
            Event.Latch::class,
            Event.Temperature::class,
        ),
    )

    val outlet = Capabilities(
        archetypeId = "usonia.smartthings.outlet",
        actions = setOf(
            Action.Switch::class,
        ),
        events = setOf(
            Event.Switch::class,
        ),
    )
}
