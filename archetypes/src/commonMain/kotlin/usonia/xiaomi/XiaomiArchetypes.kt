package usonia.xiaomi

import usonia.foundation.Capabilities
import usonia.foundation.Event
import kotlin.time.Duration.Companion.days

object XiaomiArchetypes {
    val temperature = Capabilities(
        archetypeId = "usonia.xiaomi.temperature",
        events = setOf(
            Event.Temperature::class,
            Event.Humidity::class,
            Event.Battery::class,
        ),
        heartbeat = 2.days,
    )
}
