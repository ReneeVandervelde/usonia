package usonia.frontend.navigation

import androidx.compose.runtime.Composable
import usonia.foundation.ParameterBag

/**
 * A Primary Navigation section to be rendered on the page.
 */
interface NavigationSection {
    /**
     * Configuration for when to display the page.
     */
    val route: Routing

    /**
     * Page content to show when displayed.
     */
    @Composable
    fun renderContent(args: ParameterBag)
}

