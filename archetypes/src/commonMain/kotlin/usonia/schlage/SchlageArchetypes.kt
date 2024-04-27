package usonia.schlage

import usonia.foundation.Action
import usonia.foundation.Capabilities
import usonia.foundation.Event

object SchlageArchetypes {
    val connectLock = Capabilities(
        archetypeId = "usonia.schlage.connectlock",
        events = setOf(
            Event.Lock::class,
        ),
        actions = setOf(
            Action.Lock::class
        )
    )
}
