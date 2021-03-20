package usonia.hue

import usonia.foundation.Action
import usonia.foundation.Capabilities
import usonia.foundation.Event

object HueArchetypes {
    val group = Capabilities(
        archetypeId = "usonia.hue.group",
        actions = setOf(
            Action.Switch::class,
            Action.Dim::class,
            Action.ColorTemperatureChange::class,
            Action.ColorChange::class,
        ),
        events = setOf(
            Event.Switch::class,
        )
    )

    val color = Capabilities(
        archetypeId = "usonia.hue.color",
        actions = setOf(
            Action.Switch::class,
            Action.Dim::class,
            Action.ColorTemperatureChange::class,
            Action.ColorChange::class,
        ),
        events = setOf(
            Event.Switch::class,
        )
    )
}
