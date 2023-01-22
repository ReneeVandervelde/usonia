package usonia.frontend.navigation

import androidx.compose.runtime.State
import kotlin.reflect.KClass

/**
 * Controls navigation and its current state.
 */
interface NavigationContainer {
    val currentSection: State<NavigationSection>
    fun navigateTo(target: KClass<out NavigationSection>)
    fun navigateTo(target: NavigationSection)
}
