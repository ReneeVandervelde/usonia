package usonia.smartthings

import usonia.foundation.Action
import usonia.foundation.Capabilities
import usonia.foundation.Event
import kotlin.time.Duration.Companion.days

object SmartThingsArchetypes {
    val ALL get() = setOf(
        motion,
        humidity,
        moisture,
        multi,
        outlet,
    )

    val motion = Capabilities(
        archetypeId = "usonia.smartthings.motion",
        actions = emptySet(),
        events = setOf(
            Event.Motion::class,
            Event.Temperature::class,
            Event.Battery::class,
        ),
        heartbeat = 2.days,
    )

    val humidity = Capabilities(
        archetypeId = "usonia.smartthings.humidity",
        actions = emptySet(),
        events = setOf(
            Event.Temperature::class,
            Event.Humidity::class,
        ),
        heartbeat = 5.days,
    )

    val moisture = Capabilities(
        archetypeId = "usonia.smartthings.moisture",
        actions = emptySet(),
        events = setOf(
            Event.Temperature::class,
            Event.Water::class,
        ),
        heartbeat = 5.days,
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
        heartbeat = 3.days,
    )

    val outlet = Capabilities(
        archetypeId = "usonia.smartthings.outlet",
        actions = setOf(
            Action.Switch::class,
        ),
        events = setOf(
            Event.Switch::class,
            Event.Power::class,
        ),
    )
}
