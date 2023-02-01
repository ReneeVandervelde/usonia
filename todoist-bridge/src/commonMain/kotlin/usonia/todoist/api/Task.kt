package usonia.todoist.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Task(
    val id: String,
    val content: String,
    @SerialName("is_completed")
    val completed: Boolean,
    @SerialName("project_id")
    val projectId: String? = null,
    @SerialName("labels")
    val labels: List<String> = emptyList(),
    val description: String? = null,
)
