package usonia.telegram.commands

import com.inkapplications.telegram.structures.Update
import usonia.foundation.User

interface Command {
    val id: String
    val description: String
    val visibility: Visibility get() = Visibility.Public

    suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User)

    enum class Visibility {
        Hidden,
        Public,
    }
}
