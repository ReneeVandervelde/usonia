package usonia.frontend.auth

import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import usonia.auth.Auth.Psk
import usonia.client.AuthenticationProvider

/**
 * Provides authentication for the web frontend.
 */
class WebAuthentication: AuthenticationProvider
{
    private val pskState = MutableStateFlow(localStorage.getItem("psk"))
    val isAuthenticated: Flow<Boolean> = pskState.map { it != null }

    fun login(psk: String)
    {
        localStorage.setItem("psk", psk)
        pskState.value = psk
    }

    override val bridgeIdentifier: String = "home.bridges.web"

    override val auth: Psk? get() = pskState.value?.let { Psk(it) }
}
