package usonia.todoist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TaskParameters(
    val content: String,
    @SerialName("project_id")
    val projectId: Long? = null,
    @SerialName("label_ids")
    val labels: List<Long>? = null,
)
