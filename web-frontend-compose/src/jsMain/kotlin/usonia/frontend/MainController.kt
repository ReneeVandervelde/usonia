package usonia.frontend

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import usonia.frontend.navigation.NavigationContainer
import usonia.frontend.navigation.NavigationSection
import kotlin.reflect.KClass

class MainController(
    val sections: List<NavigationSection>,
): NavigationContainer {
    private val mutableCurrentSection = mutableStateOf(sections.first())
    override val currentSection: State<NavigationSection> = mutableCurrentSection

    override fun navigateTo(target: KClass<out NavigationSection>) {
        sections.single { it::class == target }.run(::navigateTo)
    }

    override fun navigateTo(target: NavigationSection) {
        mutableCurrentSection.value = target
    }
}
