package usonia.core.state.memory

import usonia.foundation.Event
import usonia.foundation.Identifier
import kotlin.reflect.KClass

/**
 * Internal implementation of in-memory event storage.
 */
internal expect class StateMap(): MutableMap<Pair<Identifier, KClass<out Event>>, Event>
