package usonia.frontend.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.inkapplications.coroutines.ongoing.OngoingFlow

@Composable
fun <T : R, R>  OngoingFlow<T>.collectAsState(initial: R) = asFlow().collectAsState(initial)
