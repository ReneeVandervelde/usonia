package usonia.celestials

import com.inkapplications.coroutines.ongoing.collect
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import com.inkapplications.datetime.ZonedClock
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import usonia.celestials.doubles.KnownCelestials
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.FakeSite
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class JvmCelestialAccessTest
{
    private val backendClient = DummyClient.copy(
        configurationAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site = ongoingFlowOf(FakeSite.copy(
                location = KnownCelestials.LOCATION,
            ))
        }
    )

    @Test
    fun initialOutput()
    {
        val clock = object: Clock {
            override fun now(): Instant = KnownCelestials.FIRST_DATE.atTime(1, 0).toInstant(KnownCelestials.ZONE)
        }
        val celestialAccess = JvmCelestialAccess(
            usonia  = backendClient,
            clock = ZonedClock(clock, KnownCelestials.ZONE),
        )

        runTest {
            val collected = mutableListOf<UpcomingCelestials>()
            backgroundScope.launch {
                celestialAccess.localCelestials.collect {
                    collected.add(it)
                }
            }
            runCurrent()

            assertEquals(1, collected.size, "One update should be emitted")

            assertEquals(KnownCelestials.FIRST, collected[0].today)
            assertEquals(KnownCelestials.SECOND, collected[0].tomorrow)
        }
    }

    @Test
    fun afterEventPass()
    {
        val clock = object: Clock {
            override fun now(): Instant = KnownCelestials.FIRST_DATE.atTime(0, 0).toInstant(KnownCelestials.ZONE)
        }
        val celestialAccess = JvmCelestialAccess(
            usonia  = backendClient,
            clock = ZonedClock(clock, KnownCelestials.ZONE),
        )

        runTest {
            val collected = mutableListOf<UpcomingCelestials>()
            backgroundScope.launch {
                celestialAccess.localCelestials.collect {
                    collected.add(it)
                }
            }
            runCurrent()

            assertEquals(1, collected.size, "One update should be emitted")

            advanceTimeBy(KnownCelestials.FIRST.civilTwilight.start.instant - clock.now() + 1.minutes)

            assertEquals(2, collected.size, "New update should be emitted after clock passes first event")
            collected.forEach { schedule ->
                assertEquals(KnownCelestials.FIRST, schedule.today)
                assertEquals(KnownCelestials.SECOND, schedule.tomorrow)
            }
        }
    }
}
