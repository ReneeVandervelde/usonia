package usonia.frontend.navigation

import kotlinx.coroutines.flow.StateFlow

/**
 * Controls navigation and its current state.
 */
interface NavigationContainer {
    val sections: List<NavigationSection>
    val currentSection: StateFlow<NavigationInstructions>
    val topLevelRoutes: List<Routing.TopLevel>
}
