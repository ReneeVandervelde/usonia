package usonia.core.state

open class FullSecurityAccessSpy: FullSecurityAccess {
    val disarms = mutableListOf<Unit>()

    override suspend fun disarmSecurity() {
        disarms += Unit
    }
}
