package usonia.rules

import regolith.processes.daemon.DaemonFailureHandler
import regolith.processes.daemon.DaemonRunAttempt
import regolith.processes.daemon.FailureSignal

object DummyFailureHandler: DaemonFailureHandler {
    override suspend fun onFailure(attempts: List<DaemonRunAttempt>): FailureSignal = TODO()
}
