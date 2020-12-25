package usonia.core.state.memory

import usonia.foundation.Event
import usonia.foundation.Identifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

internal actual class StateMap: MutableMap<Pair<Identifier, KClass<out Event>>, Event> by ConcurrentHashMap()
