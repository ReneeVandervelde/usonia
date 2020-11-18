package usonia.cli

import kimchi.logger.LogLevel
import kimchi.logger.LogWriter

/**
 * Log writer that supports ANSI terminal colors.
 */
object ColorWriter: LogWriter {
    val esc: Char = 27.toChar()
    val red = "$esc[1;31m"
    val yellow = "$esc[1;33m"
    val green = "$esc[1;32m"
    val blue = "$esc[1;34m"
    val magenta = "$esc[1;35m"
    val normal = "$esc[0m"

    override fun log(level: LogLevel, message: String, cause: Throwable?) {
        when (level) {
            LogLevel.TRACE -> println("[${magenta}Trace$normal]: $message")
            LogLevel.DEBUG -> println("[${green}Debug$normal]: $message")
            LogLevel.INFO -> println("[${blue}Info$normal]: $message")
            LogLevel.WARNING -> println("[${yellow}Warning$normal]: $message")
            LogLevel.ERROR -> println("[${red}Error$normal]: $message")
        }
        if (cause != null) {
            println("Caused by")
            cause.printStackTrace()
        }
    }

    override fun shouldLog(level: LogLevel, cause: Throwable?) = true
}
