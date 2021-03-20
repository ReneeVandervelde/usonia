package usonia.frontend.users

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.browser.document
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import mustache.Mustache
import mustache.renderTemplate
import org.w3c.dom.Element
import org.w3c.dom.get
import usonia.client.HttpClient
import usonia.core.client.userPresenceStates
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.foundation.PresenceState
import usonia.frontend.ViewController
import usonia.js.addElementClickListener
import usonia.kotlin.*

class UserListController(
    private val client: HttpClient,
    logger: KimchiLogger = EmptyLogger,
): ViewController("users", logger), CoroutineScope by DefaultScope() {

    override suspend fun onBind(element: Element) {
        element.addElementClickListener("button[name=\"user-state\"]") { clicked ->
            onUserButtonClick(clicked)
        }
        client.userPresenceStates
            .onEach { (user, _) -> logger.trace("Binding new User ViewModel for ${user.name}") }
            .map { (user, event) -> UserViewModel(user, event) }
            .collect { viewModel ->
                val render = Mustache.renderTemplate("user-template", viewModel)
                val views = element.querySelectorAll(".user[data-user-id=\"${viewModel.id}\"]")

                when (views.length) {
                    0 -> element.innerHTML += render
                    1 -> element.replaceChild(
                        document.createElement("div").apply {
                            innerHTML = render
                        }.firstElementChild!!,
                        views[0]!!,
                    )
                    else -> throw IllegalStateException("Multiple user elements for ${viewModel.id}")
            }
        }
    }

    private fun onUserButtonClick(element: Element) {
        val userId = element.attributes["data-user-id"]?.value ?: return
        val state = element.attributes["value"]?.value ?: return

        val event = Event.Presence(
            source = Identifier(userId),
            timestamp = Clock.System.now(),
            state = PresenceState.valueOf(state),
        )

        launch { client.publishEvent(event) }
    }
}

