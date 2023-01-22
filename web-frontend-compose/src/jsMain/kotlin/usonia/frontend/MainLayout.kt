package usonia.frontend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.jetbrains.compose.web.attributes.href
import org.jetbrains.compose.web.dom.*

@Composable
fun MainLayout(
    controller: MainController,
) {
    Header(
        attrs = {
            classes("content-break")
        }
    ) {
        H1 {
            Text("Control Panel")
        }
        controller.sections.forEach { page ->
            A(
                attrs = {
                    href("javascript:void(0)")
                    onClick {
                        controller.navigateTo(page)
                    }
                }
            ) {
                Text(page.title)
            }
        }
    }
    Section(
        attrs = {
            classes("content-break")
        }
    ) {
        H1 {
            Text(controller.currentSection.value.title)
        }
        controller.currentSection.value.renderContent()
    }
}
