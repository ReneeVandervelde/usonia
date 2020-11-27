package usonia.foundation

data class Status(
    val code: Int,
    val message: String,
)

object Statuses {
    val SUCCESS = Status(
        code = 0,
        message = "We're fine. We're all fine here, now. Thank you. How are you?"
    )

    val UNKNOWN = Status(
        code = 1,
        message = "Something unexpected went wrong."
    )
}
