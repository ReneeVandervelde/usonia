package usonia.weather.nws

import inkapplications.spondee.spatial.GeoCoordinates

interface NwsApi {
    suspend fun getGridInfo(geoCoordinates: GeoCoordinates): GridInfo
    suspend fun getForecast(
        gridId: GridInfo.GridId,
        gridX: GridInfo.GridX,
        gridY: GridInfo.GridY,
    ): NwsForecast
    suspend fun getStations(
        gridId: GridInfo.GridId,
        gridX: GridInfo.GridX,
        gridY: GridInfo.GridY,
    ): StationList
    suspend fun getLatestObservations(
        stationId: StationProperties.StationIdentifier,
    ): NwsObservations
}
