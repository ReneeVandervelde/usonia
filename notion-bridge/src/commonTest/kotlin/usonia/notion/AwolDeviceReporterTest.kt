package usonia.notion

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccess
import usonia.core.state.EventAccessStub
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.datetime.UtcClock
import usonia.kotlin.datetime.current
import usonia.kotlin.ongoingFlowOf
import usonia.notion.api.structures.NotionResponse
import usonia.notion.api.structures.Parent
import usonia.notion.api.structures.block.Block
import usonia.notion.api.structures.block.BlockArgument
import usonia.notion.api.structures.database.DatabaseId
import usonia.notion.api.structures.page.Page
import usonia.notion.api.structures.page.PageId
import usonia.notion.api.structures.property.*
import usonia.server.DummyClient
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class AwolDeviceReporterTest {
    val config = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            rooms = setOf(FakeRooms.LivingRoom.copy(
                devices = setOf(FakeDevices.WaterSensor.copy(
                    id = Identifier("fake-sensor"),
                    name = "Fake Sensor",
                    capabilities = FakeDevices.WaterSensor.capabilities.copy(
                        heartbeat = 1.minutes,
                        events = setOf(
                            Event.Water::class,
                            Event.Battery::class,
                        )
                    )
                ))
            )),
            bridges = setOf(
                FakeBridge.copy(
                    service = "notion",
                    parameters = mapOf(
                        "token" to "test-token",
                        "database" to "666",
                    )
                )
            )
        ))
    }
    val testClient = DummyClient.copy(
        configurationAccess = config,
    )
    val time = UtcClock.current

    @Test
    fun noAwolDevices() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = time.instant
                    ) as T
                } else null
            }
        }
        val apiSpy = NotionApiSpy()
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(apiSpy, client).runCron(time.localDateTime, time.zone)

        assertEquals(0, apiSpy.archivedPages.size)
        assertEquals(0, apiSpy.createdPages.size)
        assertEquals(0, apiSpy.updatedPages.size)
    }

    @Test
    fun ticketCreated() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = time.instant - 2.minutes
                    ) as T
                } else null
            }
        }
        val apiSpy = NotionApiSpy()
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(apiSpy, client).runCron(time.localDateTime, time.zone)

        assertEquals(0, apiSpy.updatedPages.size, "No pages should be updated")
        assertEquals(0, apiSpy.archivedPages.size, "No pages should be archived")
        assertEquals(1, apiSpy.createdPages.size, "Expected one page to be created")
        apiSpy.createdPages.first().run {
            assertTrue(parent is Parent.Database)
            assertEquals("666", parent.database_id.value)
            assertEquals(5, properties.size)
            properties[NotionConfig.Properties.REF]?.let {
                assertEquals("fake-sensor", it.richTextPropertyText)
            }
            properties[NotionConfig.Properties.TAGS]?.let {
                assertEquals("Dead Battery", (it as PropertyArgument.MultiSelect).multi_select.single().name)
            }
            properties[NotionConfig.Properties.TITLE]?.let {
                assertEquals("Replace batteries in Fake Sensor", it.titlePropertyText)
            }
            properties[NotionConfig.Properties.IMPACT]?.let {
                assertEquals("High", (it as PropertyArgument.Select).select.name)
            }
            properties[NotionConfig.Properties.URGENCY]?.let {
                assertEquals("High", (it as PropertyArgument.Select).select.name)
            }
        }
    }

    @Test
    fun taskCreatedWithoutHistoryWhenStale() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
        }
        val apiSpy = NotionApiSpy()
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(apiSpy, client).runCron(time.localDateTime, time.zone)

        assertEquals(1, apiSpy.createdPages.size)
        assertEquals(0, apiSpy.updatedPages.size)
        assertEquals(0, apiSpy.archivedPages.size)
    }

    @Test
    fun notCreatedWithoutHistory() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(time.instant)
        }
        val apiSpy = NotionApiSpy()
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(apiSpy, client).runCron(time.localDateTime, time.zone)

        assertEquals(0, apiSpy.createdPages.size)
        assertEquals(0, apiSpy.updatedPages.size)
        assertEquals(0, apiSpy.archivedPages.size)
    }

    @Test
    fun taskClosed() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = time.instant
                    ) as T
                } else null
            }
        }
        val apiSpy = NotionApiSpy()
        val api = apiSpy.withFakeQueryResponse(
            createPageResponse(
                id = PageId("432"),
                parent = DatabaseId("666"),
                ref = "fake-sensor",
                tags = listOf(MultiSelectOption(
                    id = "123",
                    name = "Dead Battery"
                ))
            )
        )
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(api, client).runCron(time.localDateTime, time.zone)

        assertEquals(0, apiSpy.createdPages.size)
        assertEquals(1, apiSpy.updatedPages.size)
        assertEquals(0, apiSpy.archivedPages.size)
        apiSpy.updatedPages.single().assertPageClosed("432")
    }

    @Test
    fun stilAwol() = runTest {
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = time.instant - 2.minutes
                    ) as T
                } else null
            }
        }
        val apiSpy = NotionApiSpy()
        val api = apiSpy.withFakeQueryResponse(
            createPageResponse(
                id = PageId("432"),
                parent = DatabaseId("666"),
                ref = "fake-sensor",
                tags = listOf(MultiSelectOption(
                    id = "123",
                    name = "Dead Battery"
                ))
            )
        )
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(api, client).runCron(time.localDateTime, time.zone)

        assertEquals(0, apiSpy.createdPages.size)
        assertEquals(0, apiSpy.archivedPages.size)
        assertEquals(0, apiSpy.updatedPages.size)
    }

    @Test
    fun notClosedWithoutHistory() = runTest {
        val events = object : EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(time.instant)
        }
        val apiSpy = NotionApiSpy()
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(apiSpy, client).runCron(time.localDateTime, time.zone)

        assertEquals(0, apiSpy.createdPages.size)
        assertEquals(0, apiSpy.updatedPages.size)
        assertEquals(0, apiSpy.archivedPages.size)
    }

    @Test
    fun noClosableTask() = runTest {
        val events = object : EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = time.instant - 2.minutes
                    ) as T
                } else null
            }
        }
        val apiSpy = NotionApiSpy()
        val api = apiSpy.withFakeQueryResponse(
            createPageResponse(
                id = PageId("123"),
                parent = DatabaseId("666"),
                ref = "fake-sensor",
                tags = listOf(MultiSelectOption(
                    id = "123",
                    name = "Dead Battery"
                ))
            )
        )
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(api, client).runCron(time.localDateTime, time.zone)

        assertEquals(0, apiSpy.createdPages.size)
        assertEquals(0, apiSpy.updatedPages.size)
        assertEquals(0, apiSpy.archivedPages.size)
    }

    @Test
    fun noEvents() = runTest {
        val api = NotionApiSpy()
        val events = EventAccessStub
        val client = testClient.copy(
            eventAccess = events,
            configurationAccess = config,
        )
        val reporter = AwolDeviceReporter(
            notionClient = api,
            backendClient = client,
        )

        val daemon = launch { reporter.startDaemon() }
        advanceUntilIdle()

        assertEquals(0, api.createdPages.size)
        assertEquals(0, api.queries.size)
        daemon.cancel()
    }

    @Test
    fun lowBattery() = runTest {
        val api = NotionApiSpy()
        val events = object: EventAccess by EventAccessStub {
            override val events: OngoingFlow<Event> = ongoingFlowOf(
                FakeEvents.LowBattery.copy(
                    source = Identifier("fake-sensor"),
                ),
            )
        }
        val client = testClient.copy(
            eventAccess = events,
            configurationAccess = config,
        )
        val reporter = AwolDeviceReporter(
            notionClient = api,
            backendClient = client,
        )

        val daemon = launch { reporter.startDaemon() }
        advanceUntilIdle()

        assertEquals(1, api.queries.size)
        assertEquals(1, api.createdPages.size)
        api.createdPages.first().parent.run {
            assertTrue(this is Parent.Database)
            assertEquals("666", database_id.value)
        }
        assertEquals("fake-sensor", api.createdPages.first().properties[NotionConfig.Properties.REF]?.richTextPropertyText)
        daemon.cancel()
    }

    @Test
    fun noDuplicateLowBattery() = runTest {
        val apiSpy = NotionApiSpy()
        val api = apiSpy.withFakeQueryResponse(createPageResponse(
            id = PageId("test-page-id"),
            parent = DatabaseId("test-database"),
            ref = "test-ref",
        ))
        val events = object: EventAccess by EventAccessStub {
            override val events: OngoingFlow<Event> = ongoingFlowOf(
                FakeEvents.LowBattery.copy(
                    source = Identifier("fake-sensor"),
                ),
            )
        }
        val client = testClient.copy(
            eventAccess = events,
            configurationAccess = config,
        )
        val reporter = AwolDeviceReporter(
            notionClient = api,
            backendClient = client,
        )

        val daemon = launch { reporter.startDaemon() }
        advanceUntilIdle()

        assertEquals(1, apiSpy.queries.size)
        assertEquals(0, apiSpy.createdPages.size)
        daemon.cancel()
    }

    @Test
    fun lowBatteryCleared() = runTest {
        val apiSpy = NotionApiSpy()
        val api = apiSpy.withFakeQueryResponse(createPageResponse(
            id = PageId("test-page-id"),
            parent = DatabaseId("test-database"),
            ref = "fake-sensor",
            tags = listOf(MultiSelectOption(
                id = "test-tag-id",
                name = "Low Battery"
            )),
        ))
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Battery::class) {
                    FakeEvents.FullBattery.copy(
                        timestamp = time.instant,
                    ) as T
                } else null
            }
        }
        val client = testClient.copy(
            eventAccess = events,
            configurationAccess = config,
        )
        AwolDeviceReporter(api, client).runCron(time.localDateTime, time.zone)

        assertEquals(0, apiSpy.archivedPages.size)
        assertEquals(0, apiSpy.createdPages.size)
        assertEquals(1, apiSpy.updatedPages.size)
        apiSpy.updatedPages.single().assertPageClosed("test-page-id")
        apiSpy.updatedPages.single().also { (pageId, properties) ->
            assertEquals("test-page-id", pageId.value)
            assertEquals(1, properties.size)
            properties[NotionConfig.Properties.STATUS]?.let {
                assertEquals("Done", (it as PropertyArgument.Status).status.name)
            }
        }
    }

    @Test
    fun lowBatteryNotCleared() = runTest {
        val apiSpy = NotionApiSpy()
        val api = apiSpy.withFakeQueryResponse(createPageResponse(
            id = PageId("test-page-id"),
            parent = DatabaseId("test-database"),
            ref = "fake-sensor",
            tags = listOf(MultiSelectOption(
                id = "test-tag-id",
                name = "Low Battery"
            )),
        ))
        val time = UtcClock.current
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.Wet.copy(
                        timestamp = time.instant
                    ) as T
                } else null
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )
        AwolDeviceReporter(api, client).runCron(time.localDateTime, time.zone)

        assertEquals(0, apiSpy.archivedPages.size)
        assertEquals(0, apiSpy.createdPages.size)
        assertEquals(0, apiSpy.updatedPages.size)
    }

    @Test
    fun lowBatteryUpgraded() = runTest {
        val time = UtcClock.current
        val events = object: EventAccess by EventAccessStub {
            override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf(Instant.DISTANT_PAST)
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return if (id.value == "fake-sensor" && type == Event.Water::class) {
                    FakeEvents.LowBattery.copy(
                        timestamp = time.instant - 2.minutes
                    ) as T
                } else null
            }
        }
        val apiSpy = NotionApiSpy()
        val api = apiSpy.withFakeQueryResponse(createPageResponse(
            id = PageId("test-page-id"),
            parent = DatabaseId("test-database"),
            ref = "fake-sensor",
            tags = listOf(MultiSelectOption(
                id = "test-tag-id",
                name = "Low Battery"
            )),
        ))

        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(api, client).runCron(time.localDateTime, time.zone)

        assertEquals(0, apiSpy.archivedPages.size)
        assertEquals(0, apiSpy.createdPages.size)
        assertEquals(1, apiSpy.updatedPages.size)
        assertEquals("test-page-id", apiSpy.updatedPages.single().first.value)
    }

    private fun createPageResponse(
        id: PageId,
        parent: DatabaseId,
        ref: String,
        tags: List<MultiSelectOption> = emptyList(),
    ) = NotionResponse.ListResponse(
        results = listOf(
            Page(
                id = id,
                parent = Parent.Database(parent),
                properties = mapOf(
                    NotionConfig.Properties.REF to Property.RichText(
                        id = PropertyId("test-ref-property-id"),
                        rich_text = listOf(
                            Block.RichText(
                                text = Block.RichText.Text(
                                    content = ref,
                                ),
                                plain_text = ref,
                            )
                        )
                    ),
                    NotionConfig.Properties.TAGS to Property.MultiSelect(
                        id = PropertyId("test-tags-property-id"),
                        multi_select = tags,
                    )
                )
            )
        )
    )

    private val PropertyArgument.richTextPropertyText: String? get() {
        if (this !is PropertyArgument.RichText) return null
        val block = rich_text.single()
        if (block !is BlockArgument.RichText) return null

        return block.text.content
    }

    private val PropertyArgument.titlePropertyText: String? get() {
        if (this !is PropertyArgument.Title) return null
        val block = title.single()
        if (block !is BlockArgument.RichText) return null

        return block.text.content
    }

    private fun Pair<PageId, Map<PropertyName, PropertyArgument>>.assertPageClosed(pageId: String) {
        assertEquals(first.value, pageId)
        assertEquals(1, second.size)
        second[NotionConfig.Properties.STATUS]?.let {
            assertEquals("Done", (it as PropertyArgument.Status).status.name)
        }
    }
}

