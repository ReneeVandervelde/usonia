package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.ChatReference
import com.inkapplications.telegram.structures.InputFile
import com.inkapplications.telegram.structures.MessageParameters
import com.inkapplications.telegram.structures.StickerParameters
import usonia.foundation.Action

internal suspend fun TelegramBotClient.sendStickerWithMessage(
    chat: ChatReference,
    sticker: String,
    message: String,
) {
    sendSticker(StickerParameters(
        chatId = chat,
        sticker = InputFile.FileId(sticker),
    ))
    sendMessage(MessageParameters(
        chatId = chat,
        text = message,
    ))
}

internal val Action.Alert.Icon.asSticker get() = when (this) {
    Action.Alert.Icon.Suspicious -> "CAACAgEAAxkBAAEFUYti125ty_feBqvrqYxa3BlGcWvxpwACCwADq-ogTIxwBswQJsIBKQQ"
    Action.Alert.Icon.Flood -> "CAACAgEAAxkBAAEFUY1i125wdBRT8ktd1yaqHq5-OPqQkwACBgADGcIhTNePW9qrhvUrKQQ"
    Action.Alert.Icon.Pipes -> "CAACAgEAAxkBAAEFUY9i12515xomBAalBBiHkUSItKe9cwACGQAD4mQgTLMYXQABVF02QykE"
    Action.Alert.Icon.Bot -> "CAACAgEAAxkBAAEFU9Zi2Ijag5aMaiUN0LXzEOPcjhKJRgACHQEAAoTlaEYYrhoHPNOJzikE"
    Action.Alert.Icon.Confused -> "CAACAgEAAxkBAAEFUZxi13LNg36F6WaLBcbVSqHVPUd-JwACSwAD1guxBLtGTF8kC7YZKQQ"
    Action.Alert.Icon.Disallowed -> "CAACAgEAAxkBAAEFUYli123oWYOwJkPILV-i-7lQjAfJQAACjwAD1guxBDOYraKGvkauKQQ"
    Action.Alert.Icon.Danger -> "CAACAgEAAxkBAAEBTwdfVqxWtwzM8j-Jy5PpqyhgEyc6QgACvAAD1guxBIDur3NY71Q7GwQ"
    Action.Alert.Icon.Panic -> "CAACAgEAAxkBAAEBTwVfVqwG9c7KFNKUfm0mDEUVTfLhXQACQQAD1guxBO9Y7tNwd3SBGwQ"
    Action.Alert.Icon.Sleep -> "CAACAgMAAxkBAAEFUZpi13Ia23J8vJehFoRmtDgE5O_e3gACFgEAAmL1YwSXaKYAAS3ha_EpBA"
    Action.Alert.Icon.Wake -> "CAACAgEAAxkBAAEFUYdi123JYJY3EtPnHz731Pyjk6290AACKwAD1guxBOSLzZg9iCiqKQQ"
    Action.Alert.Icon.Entertained -> "CAACAgEAAxkBAAEFUYVi123Gaoef9EtimUJG32PgulHQuQACRwAD1guxBJjGVxH7DFlbKQQ"
}
