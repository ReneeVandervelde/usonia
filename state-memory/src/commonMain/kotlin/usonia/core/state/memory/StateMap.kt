package usonia.core.state.memory

import usonia.foundation.Event
import usonia.foundation.Uuid
import kotlin.reflect.KClass

/**
 * Internal implementation of in-memory event storage.
 */
internal expect class StateMap(): MutableMap<Pair<Uuid, KClass<out Event>>, Event>
