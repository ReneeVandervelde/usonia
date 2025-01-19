package com.inkapplications.telegram.structures

/**
 * Exceptions that can be thrown for internal or external api errors.
 */
abstract class TelegramException private constructor(
    message: String,
    cause: Throwable? = null,
): Throwable(message = message, cause = cause) {
    /**
     * An error that originated from the Telegram API
     */
    class External(
        val response: Response.Error,
    ): TelegramException(
        message = response.description ?: "Error ${response.code}",
    )

    /**
     * An error that was generated internally or unexpected.
     */
    class Internal(
        message: String,
        cause: Throwable? = null,
    ): TelegramException(
        message = message,
        cause = cause,
    )
}
