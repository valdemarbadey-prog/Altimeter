package ru.altimeter

import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.Typography.Companion.BODY_MEDIUM
import androidx.wear.protolayout.material3.Typography.Companion.TITLE_LARGE
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class AltimeterTileService : Material3TileService() {
    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val pressure = withTimeoutOrNull(1800L) {
            AltimeterData.readPressure(this@AltimeterTileService, 1700L)
        }
        val reading = pressure?.let { AltimeterData.getReading(this@AltimeterTileService, it) }
        val layout = primaryLayout(mainSlot = {
            if (reading == null) {
                text(if (pressure == null) "Барометр недоступен" else "Открой Altimeter для калибровки",
                    typography = BODY_MEDIUM)
            } else {
                text(String.format(Locale.getDefault(), "%.1f м", reading.altitudeM), typography = TITLE_LARGE)
                text(String.format(Locale.getDefault(), "Ground %.1f hPa", reading.groundHpa), typography = BODY_MEDIUM)
                text(String.format(Locale.getDefault(), "MSL %.1f hPa", reading.seaLevelHpa), typography = BODY_MEDIUM)
            }
        })
        return Tile.Builder()
            .setResourcesVersion("1")
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
    }
}
