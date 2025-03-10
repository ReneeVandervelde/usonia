package usonia.notion

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import com.inkapplications.datetime.ZonedClock
import com.reneevandervelde.notion.NotionBearerToken
import com.reneevandervelde.notion.NotionResponse
import com.reneevandervelde.notion.Parent
import com.reneevandervelde.notion.block.RichText
import com.reneevandervelde.notion.block.RichTextArgument
import com.reneevandervelde.notion.database.DatabaseId
import com.reneevandervelde.notion.database.DatabaseQuery
import com.reneevandervelde.notion.page.Page
import com.reneevandervelde.notion.page.PageIcon
import com.reneevandervelde.notion.page.PageId
import com.reneevandervelde.notion.property.*
import com.reneevandervelde.notion.testing.NotionApiSpy
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccess
import usonia.core.state.EventAccessStub
import usonia.foundation.*
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
    val time = ZonedClock.UTC.zonedDateTime()

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
        val page = apiSpy.createdPages.first()
        val parent = page.parent
        val properties = page.properties
        assertTrue(parent is Parent.Database)
        assertEquals("666", parent.database_id.value)
        assertEquals(5, properties.size)
        properties[NotionConfig.Properties.REF]?.let {
            assertEquals("fake-sensor", it.richTextPropertyText)
        }
        properties[NotionConfig.Properties.TAGS]?.let {
            val names = (it as PropertyArgument.MultiSelect).multi_select.map { it.name }
            assertTrue("Dead Battery" in names, "Dead battery tag sent")
            assertTrue("Usonia" in names, "Usonia tag sent")
        }
        properties[NotionConfig.Properties.TITLE]?.let {
            assertEquals("Replace batteries in Fake Sensor", it.titlePropertyText)
        }
        properties[NotionConfig.Properties.IMPACT]?.let {
            assertEquals("Medium", (it as PropertyArgument.Select).select.name)
        }
        properties[NotionConfig.Properties.URGENCY]?.let {
            assertEquals("High", (it as PropertyArgument.Select).select.name)
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
        val apiSpy = object: NotionApiSpy() {
            override suspend fun queryDatabase(
                token: NotionBearerToken,
                database: DatabaseId,
                query: DatabaseQuery,
            ): NotionResponse.ListResponse<Page> {
                super.queryDatabase(token, database, query)
                return createPageResponse(
                    id = PageId("432"),
                    parent = DatabaseId("666"),
                    ref = "fake-sensor",
                    tags = listOf(MultiSelectOption(
                        id = "123",
                        name = "Dead Battery"
                    ))
                )
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(apiSpy, client).runCron(time.localDateTime, time.zone)

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
        val apiSpy = object: NotionApiSpy() {
            override suspend fun queryDatabase(
                token: NotionBearerToken,
                database: DatabaseId,
                query: DatabaseQuery,
            ): NotionResponse.ListResponse<Page> {
                super.queryDatabase(token, database, query)
                return createPageResponse(
                    id = PageId("432"),
                    parent = DatabaseId("666"),
                    ref = "fake-sensor",
                    tags = listOf(MultiSelectOption(
                        id = "123",
                        name = "Dead Battery"
                    ))
                )
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(apiSpy, client).runCron(time.localDateTime, time.zone)

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
        val apiSpy = object: NotionApiSpy() {
            override suspend fun queryDatabase(
                token: NotionBearerToken,
                database: DatabaseId,
                query: DatabaseQuery,
            ): NotionResponse.ListResponse<Page> {
                super.queryDatabase(token, database, query)
                return createPageResponse(
                    id = PageId("123"),
                    parent = DatabaseId("666"),
                    ref = "fake-sensor",
                    tags = listOf(MultiSelectOption(
                        id = "123",
                        name = "Dead Battery"
                    ))
                )
            }
        }
        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(apiSpy, client).runCron(time.localDateTime, time.zone)

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
        val apiSpy = object: NotionApiSpy() {
            override suspend fun queryDatabase(
                token: NotionBearerToken,
                database: DatabaseId,
                query: DatabaseQuery,
            ): NotionResponse.ListResponse<Page> {
                super.queryDatabase(token, database, query)
                return createPageResponse(
                    id = PageId("test-page-id"),
                    parent = DatabaseId("test-database"),
                    ref = "test-ref",
                )
            }
        }
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
            notionClient = apiSpy,
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
        val apiSpy = object: NotionApiSpy() {
            override suspend fun queryDatabase(
                token: NotionBearerToken,
                database: DatabaseId,
                query: DatabaseQuery,
            ): NotionResponse.ListResponse<Page> {
                super.queryDatabase(token, database, query)
                return createPageResponse(
                    id = PageId("test-page-id"),
                    parent = DatabaseId("test-database"),
                    ref = "fake-sensor",
                    tags = listOf(MultiSelectOption(
                        id = "test-tag-id",
                        name = "Low Battery"
                    )),
                )
            }
        }
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
        AwolDeviceReporter(apiSpy, client).runCron(time.localDateTime, time.zone)

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
        val apiSpy = object: NotionApiSpy() {
            override suspend fun queryDatabase(
                token: NotionBearerToken,
                database: DatabaseId,
                query: DatabaseQuery,
            ): NotionResponse.ListResponse<Page> {
                super.queryDatabase(token, database, query)
                return createPageResponse(
                    id = PageId("test-page-id"),
                    parent = DatabaseId("test-database"),
                    ref = "fake-sensor",
                    tags = listOf(MultiSelectOption(
                        id = "test-tag-id",
                        name = "Low Battery"
                    )),
                )
            }
        }
        val time = ZonedClock.UTC.zonedDateTime()
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
        AwolDeviceReporter(apiSpy, client).runCron(time.localDateTime, time.zone)

        assertEquals(0, apiSpy.archivedPages.size)
        assertEquals(0, apiSpy.createdPages.size)
        assertEquals(0, apiSpy.updatedPages.size)
    }

    @Test
    fun lowBatteryUpgraded() = runTest {
        val time = ZonedClock.UTC.zonedDateTime()
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
        val apiSpy = object: NotionApiSpy() {
            override suspend fun queryDatabase(
                token: NotionBearerToken,
                database: DatabaseId,
                query: DatabaseQuery,
            ): NotionResponse.ListResponse<Page> {
                super.queryDatabase(token, database, query)
                return createPageResponse(
                    id = PageId("test-page-id"),
                    parent = DatabaseId("test-database"),
                    ref = "fake-sensor",
                    tags = listOf(MultiSelectOption(
                        id = "test-tag-id",
                        name = "Low Battery"
                    )),
                )
            }
        }

        val client = testClient.copy(
            eventAccess = events,
        )

        AwolDeviceReporter(apiSpy, client).runCron(time.localDateTime, time.zone)

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
                icon = PageIcon.Emoji("\uD83E\uDEAB"),
                parent = Parent.Database(parent),
                properties = mapOf(
                    NotionConfig.Properties.REF to Property.RichText(
                        id = PropertyId("test-ref-property-id"),
                        rich_text = listOf(
                            RichText.Text(
                                text = RichText.Text.TextContent(
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
                ),
                url = "https://reneevandervelde.com/test"
            )
        )
    )

    private val PropertyArgument.richTextPropertyText: String? get() {
        if (this !is PropertyArgument.RichText) return null
        val block = rich_text.single()
        if (block !is RichTextArgument.Text) return null

        return block.text.content
    }

    private val PropertyArgument.titlePropertyText: String? get() {
        if (this !is PropertyArgument.Title) return null
        val block = title.single()
        if (block !is RichTextArgument.Text) return null

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

