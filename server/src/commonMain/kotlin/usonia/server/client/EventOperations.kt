package usonia.server.client

import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.scalar.toWholePercentage
import inkapplications.spondee.structure.plus
import usonia.foundation.Event
import usonia.foundation.findDevice
import usonia.kotlin.first

/**
 * Creates a copy of an event with the data points adjusted.
 *
 * This should be used on incoming events to the server, so that they
 * are stored with the corrected values.
 */
suspend fun BackendClient.adjustForOffsets(data: Event): Event {
    val device = site.first().findDevice(data.source) ?: return data
    return when (data) {
        is Event.Temperature -> data.copy(
            temperature = device.parameters["temperatureOffset"]
                ?.toFloatOrNull()
                ?.fahrenheit
                ?.let { data.temperature.toFahrenheit().plus(it) }
                ?: data.temperature,
        )
        is Event.Humidity -> data.copy(
            humidity = device.parameters["humidityOffset"]
                ?.toFloatOrNull()
                ?.percent
                ?.let { data.humidity.toWholePercentage().plus(it) }
                ?: data.humidity,
        )
        else -> data
    }
}
