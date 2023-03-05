package usonia.kotlin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Filters a boolean flow to execute only when the value is true.
 */
fun Flow<Boolean>.filterTrue(): Flow<Unit> = filter { it }.map { Unit }
