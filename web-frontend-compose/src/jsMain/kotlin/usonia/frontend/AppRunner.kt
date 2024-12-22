package usonia.frontend

import androidx.compose.runtime.collectAsState
import org.jetbrains.compose.web.renderComposable
import regolith.init.Initializer
import regolith.init.TargetManager
import usonia.auth.AuthInit
import usonia.frontend.auth.WebAuthentication
import usonia.frontend.auth.LoginLayout
import usonia.frontend.navigation.NavigationContainer

/**
 * Launches the main rendering of the application with a compose element.
 */
class AppRunner(
    private val authentication: WebAuthentication,
    private val navigationContainer: NavigationContainer,
): Initializer {
    override suspend fun initialize(targetManager: TargetManager) {
        targetManager.awaitTarget(AuthInit::class)

        renderComposable(rootElementId = "compose-root") {
            if (authentication.isAuthenticated.collectAsState(false).value) {
                MainLayout(
                    controller = navigationContainer,
                )
            } else {
                LoginLayout(
                    authentication = authentication,
                )
            }
        }
    }
}
