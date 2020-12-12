package usonia.core.state.memory

import usonia.foundation.Event
import usonia.foundation.Uuid
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

internal actual class StateMap: MutableMap<Pair<Uuid, KClass<out Event>>, Event> by ConcurrentHashMap()
