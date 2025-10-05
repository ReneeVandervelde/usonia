package usonia.weather.nws

import kotlinx.serialization.Serializable

@Serializable
data class GridInfo(
    val properties: Properties,
) {
    @Serializable
    data class Properties(
        val gridId: GridId,
        val gridX: GridX,
        val gridY: GridY,
    ) {
        val coordinate get() = GridCoordinate(gridId, gridX, gridY)
    }

    @JvmInline
    @Serializable
    value class GridId(val value: String)

    @JvmInline
    @Serializable
    value class GridX(val value: Int)

    @JvmInline
    @Serializable
    value class GridY(val value: Int)

    data class GridCoordinate(
        val gridId: GridId,
        val gridX: GridX,
        val gridY: GridY,
    )
}
