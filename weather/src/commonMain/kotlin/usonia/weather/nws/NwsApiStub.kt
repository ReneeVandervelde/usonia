package usonia.weather.nws

import inkapplications.spondee.spatial.GeoCoordinates

class NwsApiStub: NwsApi
{
    override suspend fun getGridInfo(geoCoordinates: GeoCoordinates): GridInfo = TODO()
    override suspend fun getForecast(gridId: GridInfo.GridId, gridX: GridInfo.GridX, gridY: GridInfo.GridY): NwsForecast = TODO()
    override suspend fun getStations(gridId: GridInfo.GridId, gridX: GridInfo.GridX, gridY: GridInfo.GridY): List<NwsStation> = TODO()
    override suspend fun getLatestObservations(stationId: NwsStation.StationIdentifier): NwsObservations = TODO()
}
