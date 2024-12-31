package usonia.foundation

import inkapplications.spondee.spatial.GeoCoordinates
import inkapplications.spondee.spatial.latitude
import inkapplications.spondee.spatial.longitude
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal class GeoCoordinatesSerializer: KSerializer<GeoCoordinates>
{
    @Serializable
    private data class Surrogate(
        val latitude: Double,
        val longitude: Double,
    )

    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): GeoCoordinates
    {
        val surrogate = Surrogate.serializer().deserialize(decoder)
        return GeoCoordinates(
            latitude = surrogate.latitude.latitude,
            longitude = surrogate.longitude.longitude,
        )
    }

    override fun serialize(encoder: Encoder, value: GeoCoordinates)
    {
        val surrogate = Surrogate(
            latitude = value.latitude.asDecimal,
            longitude = value.longitude.asDecimal,
        )
        Surrogate.serializer().serialize(encoder, surrogate)
    }

    object Defaults
    {
        val CENTRAL_US: GeoCoordinates = GeoCoordinates(
            latitude = 39.8283.latitude,
            longitude = (-98.5795).longitude,
        )
    }
}
