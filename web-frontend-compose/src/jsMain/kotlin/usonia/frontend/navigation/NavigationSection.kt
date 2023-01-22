package usonia.frontend.navigation

import androidx.compose.runtime.Composable

/**
 * A Primary Navigation section to be rendered on the page.
 */
interface NavigationSection {
    /**
     * Title to use in links and Heading for the page.
     */
    val title: String

    /**
     * Page content to show when displayed.
     */
    @Composable
    fun renderContent()
}
