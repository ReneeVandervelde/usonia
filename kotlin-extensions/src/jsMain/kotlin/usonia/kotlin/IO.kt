package usonia.kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

actual fun IoScope(): CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
