package usonia.frontend.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import usonia.kotlin.OngoingFlow

@Composable
fun <T> OngoingFlow<T>.collectAsState(initial: T) = asFlow().collectAsState(initial)
