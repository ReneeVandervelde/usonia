package usonia.hue

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import usonia.foundation.*
import usonia.state.ConfigurationAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConfigurationTokenStorageTest {
    @Test
    fun getToken() = runBlockingTest {
        val fakeConfiguration = object: ConfigurationAccess {
            override val site: Flow<Site> = flowOf(
                FakeSite.copy(
                    bridges = setOf(
                        FakeDevices.FakeHueBridge.copy(
                            token = "test-token"
                        )
                    )
                )
            )
        }
        val storage = ConfigurationTokenStorage(fakeConfiguration)

        val result = storage.getToken()

        assertEquals("test-token", result)
    }

    @Test
    fun noToken() = runBlockingTest {
        val fakeConfiguration = object: ConfigurationAccess {
            override val site: Flow<Site> = flowOf(FakeSite)
        }
        val storage = ConfigurationTokenStorage(fakeConfiguration)

        val result = storage.getToken()

        assertNull(result)
    }
}