package usonia.frontend.navigation

import usonia.foundation.ParameterBag

data class NavigationInstructions(
    val section: NavigationSection,
    val args: ParameterBag,
)
