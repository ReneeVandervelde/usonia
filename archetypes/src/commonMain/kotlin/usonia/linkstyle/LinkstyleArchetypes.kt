package usonia.linkstyle

import usonia.foundation.Action
import usonia.foundation.Capabilities
import usonia.foundation.Event
import kotlin.time.Duration.Companion.days

object LinkstyleArchetypes {
    val valve = Capabilities(
        archetypeId = "usonia.linkstyle.valve",
        events = setOf(
            Event.Switch::class,
            Event.Valve::class,
            Event.Battery::class,
        ),
        actions = setOf(
            Action.Switch::class,
            Action.Valve::class,
        ),
        heartbeat = 5.days,
    )
}
