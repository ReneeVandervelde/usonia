package usonia.frontend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.jetbrains.compose.web.attributes.href
import org.jetbrains.compose.web.dom.*
import usonia.frontend.navigation.NavigationContainer
import usonia.frontend.navigation.NavigationInstructions

@Composable
fun MainLayout(
    controller: NavigationContainer,
) {
    Header(
        attrs = {
            classes("content-break")
        }
    ) {
        H1 {
            Text("Control Panel")
        }
        controller.topLevelRoutes.forEach { route ->
            A(
                attrs = {
                    href(route.route)
                }
            ) {
                Text(route.title)
            }
        }
    }
    Section(controller.currentSection.collectAsState().value)
}

@Composable
private fun Section(instructions: NavigationInstructions) {
    Section(
        attrs = {
            classes("content-break")
        }
    ) {
        instructions.section.renderContent(instructions.args)
    }
}
