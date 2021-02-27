package usonia.server.cron

import usonia.kotlin.datetime.ZonedDateTime

/**
 * A job that is executed periodically on a defined schedule.
 */
interface CronJob {
    /**
     * Time(s) to run the job.
     */
    val schedule: Schedule

    /**
     * Job to be executed when [schedule] matches.
     */
    suspend fun runCron(time: ZonedDateTime)

    /**
     * Optional start-up procedure.
     */
    suspend fun primeCron() = Unit
}

