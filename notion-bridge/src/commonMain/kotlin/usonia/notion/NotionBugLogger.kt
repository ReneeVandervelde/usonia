package usonia.notion

import com.inkapplications.coroutines.ongoing.*
import com.reneevandervelde.notion.NotionApi
import com.reneevandervelde.notion.NotionBearerToken
import com.reneevandervelde.notion.Parent
import com.reneevandervelde.notion.block.BlockArgument
import com.reneevandervelde.notion.block.CodeLanguage
import com.reneevandervelde.notion.block.RichTextArgument
import com.reneevandervelde.notion.block.RichTextArgument.Text
import com.reneevandervelde.notion.database.DatabaseId
import com.reneevandervelde.notion.database.DatabaseQuery
import com.reneevandervelde.notion.page.*
import com.reneevandervelde.notion.property.MultiSelectArgument
import com.reneevandervelde.notion.property.PropertyArgument
import com.reneevandervelde.notion.property.SelectArgument
import kimchi.logger.LogLevel
import kimchi.logger.LogWriter
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import regolith.processes.daemon.Daemon
import usonia.core.state.findBridgeByServiceTag
import usonia.server.client.BackendClient

internal class NotionBugLogger(
    private val notion: NotionApi,
): Daemon, LogWriter {
    private val logs = MutableSharedFlow<LogData>(extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    internal val client = MutableStateFlow<BackendClient?>(null)
    private val service = client.filterNotNull()
        .asOngoing()
        .map { it.findBridgeByServiceTag(NotionConfig.SERVICE) }
        .filterNotNull()
    private val token = service
        .map { it.parameters[NotionConfig.TOKEN]?.let(::NotionBearerToken) }
        .filterNotNull()
    private val database = service
        .map { it.parameters[NotionConfig.DATABASE]?.let(::DatabaseId) }
        .filterNotNull()
    private val data = combine(
        token,
        database,
        logs.asOngoing(),
    ) { token, database, log ->
        Parameters(token, database, log)
    }

    override suspend fun startDaemon(): Nothing
    {
        data.collectLatest { params -> handleError(params) }
    }

    private suspend fun handleError(parameters: Parameters)
    {
        val title = "Error: ${parameters.log.message.take(60)}"
        val existing = notion.queryDatabase(
            token = parameters.token,
            database = parameters.database,
            query = DatabaseQuery(
                filter = PageFilter.And(
                    filters = listOf(
                        PageFilter.Text(
                            property = NotionConfig.Properties.TITLE,
                            filter = TextFilter.Equals(title)
                        ),
                        PageFilter.Status(
                            property = NotionConfig.Properties.STATUS,
                            filter = ValueFilter.DoesNotEqual(
                                value = NotionConfig.PropertyValues.STATUS_DONE
                            )
                        ),
                        PageFilter.MultiSelect(
                            property = NotionConfig.Properties.TAGS,
                            filter = ValueFilter.Contains(NotionConfig.Tags.BUG)
                        )
                    )
                )
            )
        )

        if (existing.results.isNotEmpty()) {
            return
        }

        notion.createPage(
            token = parameters.token,
            page = NewPage(
                parent = Parent.Database(parameters.database),
                icon = PageIcon.Emoji("\uD83D\uDC1B"),
                properties = mapOf(
                    NotionConfig.Properties.TITLE to PropertyArgument.Title(
                        title = listOf(
                            Text(
                                text = Text.TextContent(
                                    content = title
                                )
                            )
                        )
                    ),
                    NotionConfig.Properties.TAGS to PropertyArgument.MultiSelect(
                        multi_select = listOf(
                            MultiSelectArgument(
                                name = NotionConfig.Tags.BUG
                            ),
                            MultiSelectArgument(
                                name = NotionConfig.Tags.USONIA
                            ),
                        )
                    ),
                    NotionConfig.Properties.IMPACT to PropertyArgument.Select(
                        select = SelectArgument(
                            name = NotionConfig.ImpactValues.MEDIUM
                        )
                    ),
                    NotionConfig.Properties.URGENCY to PropertyArgument.Select(
                        select = SelectArgument(
                            name = NotionConfig.UrgencyValues.MEDIUM
                        )
                    )
                ),
                children = listOfNotNull(
                    BlockArgument.Paragraph(
                        richText = listOf(
                            Text(
                                text = Text.TextContent(
                                    content = "Error: ${parameters.log.message}"
                                ),
                            ),
                        ),
                    ),
                    BlockArgument.Code(
                        language = CodeLanguage.PLAIN_TEXT,
                        content = listOf(
                            Text(
                                text = Text.TextContent(
                                    content = parameters.log.cause
                                        ?.stackTraceToString()
                                        .orEmpty()
                                        .take(2000)
                                ),
                            ),
                        ),
                    ).takeIf { parameters.log.cause != null },
                ),
            )
        )
    }

    override fun log(level: LogLevel, message: String, cause: Throwable?)
    {
        logs.tryEmit(LogData(message, cause))
    }

    override fun shouldLog(level: LogLevel, cause: Throwable?): Boolean
    {
        return level >= LogLevel.ERROR
    }

    private data class LogData(
        val message: String,
        val cause: Throwable?,
    )

    private data class Parameters(
        val token: NotionBearerToken,
        val database: DatabaseId,
        val log: LogData,
    )
}
