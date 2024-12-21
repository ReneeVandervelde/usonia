package usonia.server.auth

sealed interface AuthResult
{
    sealed class Failure(val message: String): AuthResult
    {
        data object IllegalSignature: Failure("Illegal/Missing Authorization Signature")
        data object IllegalTimestamp: Failure("Illegal/Missing Timestamp")
        data object IllegalBridgeId: Failure("Illegal/Missing Bridge ID")
        data object UnauthorizedBridge: Failure("Bridge not authorized")
        data object InvalidAuthorization: Failure("Invalid Authorization.")

        override fun toString(): String = "Failure($message)"
    }

    object Success: AuthResult
}
