package usonia.frontend

import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import usonia.frontend.navigation.*
import usonia.kotlin.DefaultScope

class MainController(override val sections: List<NavigationSection>): NavigationContainer {
    override val topLevelRoutes = sections.map { it.route }.filterIsInstance<Routing.TopLevel>()
    private val dynamicRoutes = sections.map { it.route }.filterIsInstance<Routing.Dynamic>()
    private val defaultSection = sections.first { it.route is Routing.TopLevel }


    override val currentSection: StateFlow<NavigationInstructions> = callbackFlow {
        val clickInterceptor = object: EventListener {
            override fun handleEvent(event: Event) {
                window.history.pushState(
                    data = null,
                    title = "",
                    url = (event.target as? HTMLElement)?.getAttribute("href") ?: return
                )
                event.preventDefault()
                event.stopPropagation()
                trySend(currentNavigationInstructions())
            }
        }
        val popStateListener = object: EventListener {
            override fun handleEvent(event: Event) {
                trySend(currentNavigationInstructions())
            }
        }
        window.addEventListener("click", clickInterceptor)
        window.addEventListener("popstate", popStateListener)

        awaitClose {
            window.removeEventListener("click", clickInterceptor)
            window.removeEventListener("popstate", popStateListener)
        }
    }.stateIn(DefaultScope(), SharingStarted.Eagerly, currentNavigationInstructions())

    private fun currentNavigationInstructions(): NavigationInstructions {
        val current = window.location.pathname
        val topLevelMatch = topLevelRoutes.firstOrNull { it.route == current }
        val dynamicMatch = dynamicRoutes.firstOrNull { it.pathRegex.matches(current) }
        return when {
            topLevelMatch != null -> sections.first { it.route === topLevelMatch }
                .let { NavigationInstructions(it, emptyMap()) }

            dynamicMatch != null -> sections.first { it.route === dynamicMatch }
                .let {
                    val groups = dynamicMatch.pathRegex.matchEntire(current)!!.groups
                    NavigationInstructions(
                        section = it,
                        args = dynamicMatch.groupNames.map { it to groups.get(it)!!.value }.toMap(),
                    )
                }

            else -> NavigationInstructions(defaultSection, emptyMap())
        }
    }
}
