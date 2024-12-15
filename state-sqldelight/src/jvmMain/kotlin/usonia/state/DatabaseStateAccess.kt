package usonia.state

import com.inkapplications.coroutines.filterItemSuccess
import com.inkapplications.coroutines.mapItemsCatching
import com.inkapplications.coroutines.onItemFailure
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.structure.toFloat
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import regolith.data.settings.SettingLevel
import regolith.data.settings.SettingsAccess
import regolith.data.settings.observeSetting
import regolith.data.settings.structure.StringData
import regolith.data.settings.structure.createKeyedEnumTransformer
import regolith.data.settings.writeSetting
import usonia.foundation.*
import usonia.foundation.Event
import usonia.foundation.Site
import usonia.kotlin.OngoingFlow
import usonia.kotlin.asOngoing
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

/**
 * The default limit to use when querying potentially large collections.
 */
private const val DEFAULT_COLLECTION_SIZE = 100L

/**
 * Default time limit to use for historical queries.
 */
private val DEFAULT_HISTORY_DURATION = 7.days

/**
 * Implement database services via SQLite
 */
internal class DatabaseStateAccess(
    private val eventQueries: Lazy<EventQueries>,
    private val siteQueries: Lazy<SiteQueries>,
    private val flagQueries: Lazy<FlagQueries>,
    private val settingsAccess: SettingsAccess,
    private val json: Json,
    private val zonedClock: ZonedClock = ZonedSystemClock,
    private val logger: KimchiLogger = EmptyLogger,
): DatabaseServices {
    private val securityStateSetting = StringData(
        key = "usonia.state.security",
        name = "Security State",
        dataTransformer = createKeyedEnumTransformer(),
        defaultValue = SecurityState.Disarmed,
        level = SettingLevel.Hidden,
    )
    private val eventsFlow = MutableSharedFlow<Event>()
    override val securityState: OngoingFlow<SecurityState> = settingsAccess
        .observeSetting(securityStateSetting)
        .asOngoing()

    override suspend fun armSecurity() {
        settingsAccess.writeSetting(securityStateSetting, SecurityState.Armed)
    }

    override suspend fun disarmSecurity() {
        settingsAccess.writeSetting(securityStateSetting, SecurityState.Disarmed)
    }

    override val events: OngoingFlow<Event> = eventsFlow.asOngoing()
    override val eventsByDay: OngoingFlow<Map<LocalDate, Int>> by lazy {
        eventQueries.value.eventsByDay()
            .asFlow()
            .mapToList()
            .map {
                it.map {
                    LocalDate.parse(it.localdate) to it.total.toInt()
                }.toMap()
            }
            .asOngoing()
    }

    override val oldestEventTime: OngoingFlow<Instant?> by lazy {
        eventQueries.value
            .oldestEvent()
            .asFlow()
            .mapToOneOrNull()
            .map {
                it?.let { Instant.fromEpochMilliseconds(it) }
            }
            .asOngoing()
    }

    override val site: OngoingFlow<Site> by lazy {
        siteQueries.value
            .latest()
            .asFlow()
            .mapToOneOrNull()
            .filterNotNull()
            .map {
                it.data_.let(::String).let { json.decodeFromString(Site.serializer(), it) }
            }
            .asOngoing()
    }

    override val flags: OngoingFlow<Map<String, String?>> by lazy {
        flagQueries.value
            .list()
            .asFlow()
            .mapToList()
            .map {
                it.map {
                    it.id to it.data_
                }.toMap()
            }
            .asOngoing()
    }

    override suspend fun publishEvent(event: Event) {
        eventsFlow.emit(event)
        eventQueries.value.insert(
            timestamp = event.timestamp.toEpochMilliseconds(),
            source = event.source.value,
            type = event::class.simpleName!!,
            category = event.category.name,
            data_ = json.encodeToString(Event.serializer(), event).toByteArray(),
        )
    }

    override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
        return eventQueries.value.latest(type.simpleName!!, id.value) { _, _, _, _, _, data ->
            json.decodeFromString(Event.serializer(), String(data))
        }.executeAsOneOrNull() as T?
    }

    override fun temperatureHistorySnapshots(devices: Collection<Identifier>, limit: Duration?): OngoingFlow<List<TemperatureSnapshot>> {
        return devices
            .map { it.value }
            .let {
                eventQueries.value.eventsBySourceAndTypeAfterTimestampInclusive(
                    source = it,
                    type = Event.Temperature::class.simpleName!!,
                    timestamp = zonedClock.now().minus(limit ?: DEFAULT_HISTORY_DURATION).toEpochMilliseconds(),
                )
            }
            .asFlow()
            .mapToList()
            .mapItemsCatching { json.decodeFromString(EventSerializer, String(it)) as Event.Temperature }
            .onItemFailure { logger.warn("Failed to deserialize temperature event", it) }
            .filterItemSuccess()
            .map {
                it.groupBy { (it.timestamp - zonedClock.now()).toInt(DurationUnit.HOURS).hours }
                    .mapValues { (_, events) ->
                        events.map { it.temperature.toFahrenheit().toFloat() }.average().fahrenheit
                    }
                    .map { (key, value) -> TemperatureSnapshot(key, value) }
                    .sortedBy { it.timeAgo }
            }
            .asOngoing()
    }

    override fun deviceEventHistory(id: Identifier, size: Int?): OngoingFlow<List<Event>> {
        return eventQueries.value.eventsBySource(setOf(id.value), size?.toLong() ?: DEFAULT_COLLECTION_SIZE)
            .asFlow()
            .mapToList()
            .mapItemsCatching { json.decodeFromString(EventSerializer, String(it)) }
            .onItemFailure { logger.warn("Failed to deserialize event", it) }
            .filterItemSuccess()
            .map { it.toList() }
            .asOngoing()
    }

    override fun eventCount(id: Identifier, category: EventCategory): OngoingFlow<Long> {
        return eventQueries.value.eventCountByDeviceAndCategory(id.value, category.name)
            .asFlow()
            .mapToOneOrNull()
            .map { it ?: 0 }
            .asOngoing()
    }

    override fun getLatestEvent(id: Identifier): OngoingFlow<Event> {
        return eventQueries.value.eventsBySource(setOf(id.value), 1)
            .asFlow()
            .mapToOneOrNull()
            .map { it?.let { json.decodeFromString(EventSerializer, String(it)) } }
            .filterNotNull()
            .asOngoing()
    }

    override suspend fun updateSite(site: Site) {
        siteQueries.value.update(json.encodeToString(Site.serializer(), site).toByteArray())
    }

    override suspend fun setFlag(key: String, value: String?) {
        flagQueries.value.update(key, value)
    }

    override suspend fun removeFlag(key: String) {
        flagQueries.value.delete(key)
    }
}
