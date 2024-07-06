package usonia.kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * An Ongoing flow is like a flow, but never ends.
 *
 * This wraps a flow, discouraging accidental use of unsafe extensions
 * for this type of data stream.
 */
interface OngoingFlow<T> {
    /**
     * Convert thee ongoing flow to a standard flow.
     */
    fun asFlow(): Flow<T>
}

/**
 * Wrapped implementation of an ongoing flow.
 */
private class WrappedOngoingFlow<T>(private val backing: Flow<T>): OngoingFlow<T> {
    override fun asFlow(): Flow<T> = backing
}

/**
 * Convert an existing flow to an ongoing flow.
 */
fun <T> Flow<T>.asOngoing(): OngoingFlow<T> = WrappedOngoingFlow(this)

/**
 * Create an ongoing flow.
 */
inline fun <T> ongoingFlow(crossinline builder: suspend FlowCollector<T>.() -> Unit): OngoingFlow<T> {
    return flow {
        builder(this)
        awaitCancellation()
    }.asOngoing()
}

/**
 * Create an ongoing flow from a set of items.
 */
fun <T> ongoingFlowOf(vararg items: T): OngoingFlow<T> {
    return ongoingFlow {
        items.forEach { emit(it) }
    }
}

/**
 * Short syntax for first item collector.
 */
suspend fun <T> OngoingFlow<T>.first(): T = asFlow().first()

/**
 * Modify an ongoing flow temporarily as a standard flow.
 *
 * This allows standard flow operators to be applied to the OngoingFlow
 *
 *     ongoingFlow.modify {
 *         filter { it != "foo" }.distinct()
 *     }
 */
inline fun <T, R> OngoingFlow<T>.unsafeModify(modifier: Flow<T>.() -> Flow<R>): OngoingFlow<R> {
    return asFlow().let(modifier).asOngoing()
}

/**
 * @see Flow.filterIsInstance
 */
inline fun <reified R> OngoingFlow<*>.filterIsInstance(): OngoingFlow<R> = unsafeModify { filterIsInstance() }

/**
 * @see Flow.filter
 */
inline fun <T> OngoingFlow<T>.filter(crossinline predicate: (T) -> Boolean) = unsafeModify { filter { predicate(it) } }

/**
 * @see Flow.map
 */
inline fun <T, R> OngoingFlow<T>.map(crossinline mapper: suspend (T) -> R) = unsafeModify { map { mapper(it) } }

/**
 * @see Flow.mapLatest
 */
@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T, R> OngoingFlow<T>.mapLatest(crossinline mapper: suspend (T) -> R) = unsafeModify { mapLatest { mapper(it) } }

/**
 * @see Flow.flatMapLatest
 */
@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T, R> OngoingFlow<T>.flatMapLatest(crossinline mapper:  suspend (T) -> Flow<R>) = unsafeModify { flatMapLatest { mapper(it) } }

/**
 * @see Flow.flatMapConcat
 */
@OptIn(ExperimentalCoroutinesApi::class)
inline fun <T, R> OngoingFlow<T>.flatMapConcat(crossinline mapper:  (T) -> Flow<R>) = unsafeModify { flatMapConcat { mapper(it) } }

/**
 * @see Flow.onEach
 */
inline fun <T> OngoingFlow<T>.onEach(crossinline action: (T) -> Unit) = unsafeModify { onEach { action(it) } }

/**
 * @see Flow.combine
 */
inline fun <T1, T2, R> OngoingFlow<T1>.combineWith(other: OngoingFlow<T2>, crossinline transform: (a: T1, b: T2) -> R): OngoingFlow<R> {
    return unsafeModify {
        this.combine(other.asFlow()) { a, b ->
            transform(a, b)
        }
    }
}

/**
 * @see filterTrue
 */
fun OngoingFlow<Boolean>.filterTrue() = unsafeModify { filterTrue() }

/**
 * @see Flow.filterNotNull
 */
fun <T: Any> OngoingFlow<T?>.filterNotNull() = unsafeModify { filterNotNull() }

/**
 * @see Flow.distinctUntilChanged
 */
fun <T> OngoingFlow<T>.distinctUntilChanged() = unsafeModify { distinctUntilChanged() }

/**
 * Emits an item at the start of a flow.
 */
fun <T> OngoingFlow<T>.startWith(item: T) = unsafeModify { onStart { emit(item) } }

/**
 * @see Flow.drop
 */
fun <T> OngoingFlow<T>.drop(count: Int) = unsafeModify { drop(count) }

/**
 * Emits an item at the start of a flow only if the item is not null.
 */
fun <T: Any> OngoingFlow<T>.startWithIfNotNull(item: T?) = unsafeModify { onStart { if (item != null) emit(item) } }

/**
 * Combines two flows into a pair of data
 */
fun <T1, T2> OngoingFlow<T1>.combineToPair(other: OngoingFlow<T2>): OngoingFlow<Pair<T1, T2>> {
    return unsafeModify {
        this.combine(other.asFlow()) { a, b -> a to b }
    }
}

/**
 * Scans a flow's emissions by grouping them into a buffer of a specified size.
 *
 * ie. given the flow: `[A, B, C, D, E]`
 * calling `rollingWindow(3)` will produce: `[[A, B, C], [B, C, D], [C, D, E]]`
 */
fun <T> OngoingFlow<T>.rollingWindow(size: Int): OngoingFlow<List<T>> = unsafeModify {
    scan(emptyList<T>()) { accumulator, value: T ->
        (accumulator + value).takeLast(size)
    }.filter { it.size == size }
}

/**
 * Launches a new coroutine for collecting each emitted item.
 */
suspend inline fun <T> OngoingFlow<T>.collectOn(scope: CoroutineScope, crossinline observer: suspend (T) -> Unit): Nothing {
    asFlow().collect {
        scope.launch { observer(it) }
    }
    throw IllegalStateException("Unexpected end of flow")
}

/**
 * Collects a flow that never returns.
 *
 * This is identical to [collect], but throws an exception if the flow ends.
 */
suspend inline fun <T> OngoingFlow<T>.collect(crossinline observer: suspend (T) -> Unit): Nothing {
    asFlow().collect { observer(it) }
    throw IllegalStateException("Unexpected end of flow")
}

/**
 * Collects a flow that never returns.
 *
 * This is identical to a Flow's [collectLatest], but throws an exception if the flow ends.
 */
suspend inline fun <T> OngoingFlow<T>.collectLatest(crossinline observer: suspend (T) -> Unit): Nothing {
    asFlow().collectLatest { observer(it) }
    throw IllegalStateException("Unexpected end of flow")
}

/**
 * Analogue to [Flow.combine] for ongoing flows.
 */
fun <T1, T2, R> combine(
flow1: OngoingFlow<T1>,
    flow2: OngoingFlow<T2>,
    transform: (a: T1, b: T2) -> R,
): OngoingFlow<R> {
    return combine(
        flow1.asFlow(),
        flow2.asFlow(),
        transform
    ).asOngoing()
}

/**
 * Analogue to [Flow.combine] for ongoing flows.
 */
fun <T1, T2, T3, R> combine(
    flow1: OngoingFlow<T1>,
    flow2: OngoingFlow<T2>,
    flow3: OngoingFlow<T3>,
    transform: (a: T1, b: T2, c: T3) -> R,
): OngoingFlow<R> {
    return combine(
        flow1.asFlow(),
        flow2.asFlow(),
        flow3.asFlow(),
        transform
    ).asOngoing()
}

/**
 * Analogue to [Flow.combine] for ongoing flows.
 */
fun <T1, T2, T3, T4, R> combine(
    flow1: OngoingFlow<T1>,
    flow2: OngoingFlow<T2>,
    flow3: OngoingFlow<T3>,
    flow4: OngoingFlow<T4>,
    transform: (a: T1, b: T2, c: T3, d: T4) -> R,
): OngoingFlow<R> {
    return combine(
        flow1.asFlow(),
        flow2.asFlow(),
        flow3.asFlow(),
        flow4.asFlow(),
        transform
    ).asOngoing()
}

/**
 * Analogue to [Flow.combine] for ongoing flows.
 */
fun <T1, T2, T3, T4, T5, R> combine(
    flow1: OngoingFlow<T1>,
    flow2: OngoingFlow<T2>,
    flow3: OngoingFlow<T3>,
    flow4: OngoingFlow<T4>,
    flow5: OngoingFlow<T5>,
    transform: (a: T1, b: T2, c: T3, d: T4, e: T5) -> R,
): OngoingFlow<R> {
    return combine(
        flow1.asFlow(),
        flow2.asFlow(),
        flow3.asFlow(),
        flow4.asFlow(),
        flow5.asFlow(),
        transform
    ).asOngoing()
}
