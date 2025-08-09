package usonia.foundation

import com.github.ajalt.colormath.Color
import com.github.ajalt.colormath.model.RGB
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.roundToInt

class ColorSrgbArraySerializer: KSerializer<Color>
{
    private val surrogate = ColorArraySurrogate.serializer()
    override val descriptor: SerialDescriptor get() = surrogate.descriptor

    override fun serialize(encoder: Encoder, value: Color)
    {
        val rgb = value.toSRGB()
        val surrogateValue = ColorArraySurrogate(rgb = listOf(rgb.r.roundToInt(), rgb.g.roundToInt(), rgb.b.roundToInt()))
        surrogate.serialize(encoder, surrogateValue)
    }

    override fun deserialize(decoder: Decoder): Color
    {
        val surrogateValue = surrogate.deserialize(decoder)
        return RGB.Companion(surrogateValue.rgb[0], surrogateValue.rgb[1], surrogateValue.rgb[2])
    }

    @JvmInline
    @Serializable
    value class ColorArraySurrogate(val rgb: List<Int>)
}
