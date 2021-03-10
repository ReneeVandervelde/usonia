package usonia.xiaomi

import usonia.foundation.Capabilities
import usonia.foundation.Event

object XiaomiArchetypes {
    val temperature = Capabilities(
        archetypeId = "usonia.xiaomi.temperature",
        events = setOf(
            Event.Temperature::class,
            Event.Humidity::class,
            Event.Battery::class,
        ),
    )
}
