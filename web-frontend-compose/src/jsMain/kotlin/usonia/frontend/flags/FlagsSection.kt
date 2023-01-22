package usonia.frontend.flags

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import usonia.client.HttpClient
import usonia.frontend.extensions.collectAsState
import usonia.frontend.navigation.NavigationSection

class FlagsSection(
    private val client: HttpClient,
    private val backgroundScope: CoroutineScope,
): NavigationSection {
    override val title: String = "Flags"

    @Composable
    override fun renderContent() {
        client.flags.collectAsState(emptyMap())
            .value
            .toList()
            .sortedBy { it.first }
            .forEach { (key, value) ->
                val onFlagChange: (String?) -> Unit = {
                    onFlagChange(key, it)
                }
                when {
                    value?.lowercase() == "true" -> EnabledFlagControl(key, onFlagChange)
                    value?.lowercase() == "false" -> DisabledFlagControl(key, onFlagChange)
                    value == null -> {}
                    else -> TextFlagField(key, value, onFlagChange)
                }
            }
    }

    private fun onFlagChange(key: String, value: String?) {
        backgroundScope.launch {
            client.setFlag(key, value)
        }
    }
}
