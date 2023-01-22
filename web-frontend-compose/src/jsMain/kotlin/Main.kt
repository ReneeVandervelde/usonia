import org.jetbrains.compose.web.renderComposable
import usonia.frontend.MainLayout
import usonia.frontend.MainModule

fun main() {
    val mainModule = MainModule()
    renderComposable(rootElementId = "compose-root") {
        MainLayout(
            controller = mainModule.mainController,
        )
    }
}
