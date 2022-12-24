//package usonia.hue
//
//import kotlinx.coroutines.test.runBlockingTest
//import usonia.core.state.ConfigurationAccess
//import usonia.core.state.ConfigurationAccessStub
//import usonia.foundation.FakeDevices
//import usonia.foundation.FakeSite
//import usonia.foundation.Site
//import usonia.kotlin.OngoingFlow
//import usonia.kotlin.ongoingFlowOf
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertNull
//
//class ConfigurationTokenStorageTest {
//    @Test
//    fun getToken() = runBlockingTest {
//        val fakeConfiguration = object: ConfigurationAccess by ConfigurationAccessStub {
//            override val site: OngoingFlow<Site> = ongoingFlowOf(
//                FakeSite.copy(
//                    bridges = setOf(
//                        FakeDevices.FakeHueBridge.copy(
//                            parameters = mapOf(
//                                "token" to "test-token",
//                            ),
//                        )
//                    )
//                )
//            )
//        }
//        val storage = ConfigurationTokenStorage(fakeConfiguration)
//
//        val result = storage.getToken()
//
//        assertEquals("test-token", result)
//    }
//
//    @Test
//    fun noToken() = runBlockingTest {
//        val fakeConfiguration = object: ConfigurationAccess by ConfigurationAccessStub {
//            override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite)
//        }
//        val storage = ConfigurationTokenStorage(fakeConfiguration)
//
//        val result = storage.getToken()
//
//        assertNull(result)
//    }
//}
