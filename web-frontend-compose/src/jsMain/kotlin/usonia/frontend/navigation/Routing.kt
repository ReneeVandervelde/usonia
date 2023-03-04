package usonia.frontend.navigation

/**
 * Configuration for when to display a page.
 */
sealed interface Routing {
    /**
     * Route that can be navigated to from the main navigation.
     */
    data class TopLevel(
        val route: String,
        val title: String,
    ): Routing

    /**
     * Route that must be navigated to directly and may require parameters.
     */
    data class Dynamic(
        val route: String,
    ): Routing {
        private val placeholderRegex = Regex("\\{([a-zA-Z0-9._~-]+)\\}")
        val pathRegex = route.replace(placeholderRegex, "(?<$1>[a-zA-Z0-9._~-]+)").let(::Regex)
        val groupNames = placeholderRegex.findAll(route).map { it.groupValues[1] }.toList()
    }
}
