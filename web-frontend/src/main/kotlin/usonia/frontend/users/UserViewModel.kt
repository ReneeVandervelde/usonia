package usonia.frontend.users

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import usonia.foundation.Event
import usonia.foundation.User
import usonia.kotlin.timestapSentanceFormat

data class UserViewModel(
    val id: String,
    val name: String,
    val state: String,
    val since: String?,
) {
    constructor(user: User, latest: Event.Presence?): this(
        id = user.id.value,
        name = user.name,
        state = latest?.state?.name ?: "UNKNOWN",
        since = latest?.timestamp?.toLocalDateTime(TimeZone.currentSystemDefault())
            ?.timestapSentanceFormat
            ?: "forever"
    )
}
