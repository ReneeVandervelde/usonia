package usonia.todoist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Task(
    val id: Long,
    val content: String,
    val completed: Boolean,
    @SerialName("project_id")
    val projectId: Long? = null,
    @SerialName("label_ids")
    val labels: List<Long> = emptyList(),
)
