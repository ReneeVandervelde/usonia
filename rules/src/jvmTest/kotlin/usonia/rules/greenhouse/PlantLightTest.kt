package usonia.rules.greenhouse

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import com.inkapplications.datetime.toZonedDateTime
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone.Companion.UTC
import usonia.celestials.CelestialAccess
import usonia.celestials.FakeCelestials
import usonia.celestials.FakeUpcomingCelestials
import usonia.celestials.UpcomingCelestials
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class PlantLightTest {
    private val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            rooms = setOf(
                FakeRooms.LivingRoom.copy(
                    devices = setOf(
                        FakeDevices.Switch.copy(id = Identifier("plant-switch"), fixture = Fixture.Plant),
                        FakeDevices.Switch.copy(id = Identifier("unrelated-switch")),
                    ),
                ),
            )
        ))
    }

    @Test
    fun before() = runTest {
        val now = Instant.fromEpochMilliseconds(currentTime).toZonedDateTime(UTC)
        val sunriseTime = now + 4.hours
        val celestialAccess = object: CelestialAccess {
            override val localCelestials: OngoingFlow<UpcomingCelestials> = ongoingFlowOf(
                FakeUpcomingCelestials.copy(
                    today = FakeCelestials.copy(
                        daylight = sunriseTime..(sunriseTime + 12.hours),
                    )
                ),
            )
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = actionSpy,
        )

        PlantLight(client, celestialAccess).runCron(now.localDateTime, UTC)

        runCurrent()
        assertEquals(0, actionSpy.actions.size, "No action before sunrise")
    }

    @Test
    fun afterStart() = runTest {
        val now = Instant.fromEpochMilliseconds(currentTime).toZonedDateTime(UTC)
        val sunriseTime = now - 1.hours
        val celestialAccess = object: CelestialAccess {
            override val localCelestials: OngoingFlow<UpcomingCelestials> = ongoingFlowOf(
                FakeUpcomingCelestials.copy(
                    today = FakeCelestials.copy(
                        daylight = sunriseTime..(sunriseTime + 12.hours),
                    )
                ),
            )
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = actionSpy,
        )

        PlantLight(client, celestialAccess).runCron(now.localDateTime, UTC)

        runCurrent()
        assertEquals(1, actionSpy.actions.size, "On action sent")
        assertEquals(SwitchState.ON, (actionSpy.actions.single() as? Action.ColorTemperatureChange)?.switchState)
    }


    @Test
    fun afterSunrise() = runTest {
        val now = Instant.fromEpochMilliseconds(currentTime).toZonedDateTime(UTC)
        val sunriseTime = now - 6.hours
        val celestialAccess = object: CelestialAccess {
            override val localCelestials: OngoingFlow<UpcomingCelestials> = ongoingFlowOf(
                FakeUpcomingCelestials.copy(
                    today = FakeCelestials.copy(
                        daylight = sunriseTime..(sunriseTime + 12.hours),
                    )
                ),
            )
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = actionSpy,
        )

        PlantLight(client, celestialAccess).runCron(now.localDateTime, UTC)

        runCurrent()
        assertEquals(1, actionSpy.actions.size, "On action sent")
        assertEquals(SwitchState.ON, (actionSpy.actions.single() as? Action.ColorTemperatureChange)?.switchState)
    }

    @Test
    fun afterEnd() = runTest {
        val now = Instant.fromEpochMilliseconds(currentTime).toZonedDateTime(UTC)
        val sunriseTime = now - 12.hours
        val celestialAccess = object: CelestialAccess {
            override val localCelestials: OngoingFlow<UpcomingCelestials> = ongoingFlowOf(
                FakeUpcomingCelestials.copy(
                    today = FakeCelestials.copy(
                        daylight = sunriseTime..(sunriseTime + 12.hours),
                    )
                ),
            )
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = actionSpy,
        )

        PlantLight(client, celestialAccess).runCron(now.localDateTime, UTC)

        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Off action sent")
        assertEquals(SwitchState.OFF, (actionSpy.actions.single() as? Action.Switch)?.state)
    }
}
