package usonia.frontend.flags

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import usonia.client.HttpClient
import usonia.core.client.userPresenceStates
import usonia.foundation.Event
import usonia.foundation.ParameterBag
import usonia.foundation.PresenceState
import usonia.foundation.User
import usonia.frontend.extensions.collectAsState
import usonia.frontend.navigation.NavigationSection
import usonia.frontend.navigation.Routing

class FlagsSection(
    private val client: HttpClient,
    private val backgroundScope: CoroutineScope,
    private val clock: Clock = Clock.System,
): NavigationSection {
    override val route: Routing = Routing.TopLevel(
        route = "/flags",
        title = "Flags",
    )

    @Composable
    override fun renderContent(args: ParameterBag) {
        H1 { Text("Flags") }
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

        H1 { Text("User Presence") }
        client.userPresenceStates.collectAsState(emptyList())
            .value
            .sortedBy { it.first.name }
            .forEach { (user, presence) ->
                UserControl(user, presence?.state) { onUserUpdate(user, it) }
            }
    }

    private fun onFlagChange(key: String, value: String?) {
        backgroundScope.launch {
            client.setFlag(key, value)
        }
    }

    private fun onUserUpdate(user: User, newState: PresenceState) {
        backgroundScope.launch {
            client.publishEvent(Event.Presence(user.id, clock.now(), newState))
        }
    }
}
