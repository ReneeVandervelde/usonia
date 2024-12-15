package usonia.hue

import inkapplications.shade.structures.SecurityStrategy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.FakeBridge
import usonia.foundation.FakeSite
import usonia.foundation.Site
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LiveConfigContainerTest {
    @Test
    fun authTokenDefault() {
        val scope = TestScope()
        val container = LiveConfigContainer(ConfigurationAccessStub, scope)

        assertNull(container.authToken.value)
        scope.cancel()
    }

    @Test
    fun authTokenUpdates() {
        val scope = TestScope()
        val config = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    bridges = setOf(
                        FakeBridge.copy(
                            service = HUE_SERVICE,
                            parameters = mapOf(
                                HUE_TOKEN to "FAKE-TOKEN",
                            ),
                        ),
                    ),
                ),
            )
        }
        val container = LiveConfigContainer(config, scope)

        scope.runCurrent()
        assertEquals("FAKE-TOKEN", container.authToken.value?.applicationKey)
        scope.cancel()
    }

    @Test
    fun hostnameDefault() {
        val scope = TestScope()
        val container = LiveConfigContainer(ConfigurationAccessStub, scope)

        assertNull(container.hostname.value)
        scope.cancel()
    }

    @Test
    fun hostnameUpdates() {
        val scope = TestScope()
        val config = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    bridges = setOf(
                        FakeBridge.copy(
                            service = HUE_SERVICE,
                            parameters = mapOf(
                                HUE_URL to "FAKE-URL",
                            ),
                        ),
                    ),
                ),
            )
        }
        val container = LiveConfigContainer(config, scope)

        scope.runCurrent()
        assertEquals("FAKE-URL", container.hostname.value)
        scope.cancel()
    }

    @Test
    fun securityStrategyDefault() {
        val scope = TestScope()
        val container = LiveConfigContainer(ConfigurationAccessStub, scope)

        assertEquals(SecurityStrategy.PlatformTrust, container.securityStrategy.value)
        scope.cancel()
    }

    @Test
    fun securityStrategyUpdates() {
        val scope = TestScope()
        val config = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    bridges = setOf(
                        FakeBridge.copy(
                            service = HUE_SERVICE,
                            parameters = mapOf(
                                HUE_URL to "FAKE-URL",
                            ),
                        ),
                    ),
                ),
            )
        }
        val container = LiveConfigContainer(config, scope)

        scope.runCurrent()
        val securityStrategy = container.securityStrategy.value
        assertTrue(securityStrategy is SecurityStrategy.Insecure)
        assertEquals("FAKE-URL", securityStrategy.hostname)
        scope.cancel()
    }

    @Test
    fun disabledSetters() = runTest {
        val scope = TestScope()
        val container = LiveConfigContainer(ConfigurationAccessStub, scope)

        assertFails { container.setAuthToken(null) }
        assertFails { container.setHostname(null) }
        assertFails { container.setSecurityStrategy(SecurityStrategy.PlatformTrust) }

        scope.cancel()
    }
}
