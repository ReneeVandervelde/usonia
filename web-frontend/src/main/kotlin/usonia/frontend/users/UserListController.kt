package usonia.frontend.users

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.browser.document
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import mustache.Mustache
import org.w3c.dom.Element
import org.w3c.dom.get
import usonia.client.UsoniaClient
import usonia.client.userPresenceStates
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.foundation.PresenceState
import usonia.frontend.ViewController
import usonia.js.addElementClickListener
import usonia.kotlin.UnconfinedScope

class UserListController(
    private val client: UsoniaClient,
    private val logger: KimchiLogger = EmptyLogger,
): ViewController, CoroutineScope by UnconfinedScope() {
    private val template by lazy { document.getElementById("user-template")!!.innerHTML }
    private val container by lazy { document.getElementById("users")!! }

    override suspend fun bind() {
        container.addElementClickListener("button[name=\"user-state\"]") { element ->
            onUserButtonClick(element)
        }
        client.userPresenceStates
            .onEach { (user, _) -> logger.trace("Binding new User ViewModel for ${user.name}") }
            .map { (user, event) -> UserViewModel(user, event) }
            .collect { viewModel ->
                val render = Mustache.render(template, viewModel)
                val views = container.querySelectorAll(".user[data-user-id=\"${viewModel.id}\"]")

                when (views.length) {
                    0 -> container.innerHTML += render
                    1 -> container.replaceChild(
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

        launch { client.sendEvent(event) }
    }
}

