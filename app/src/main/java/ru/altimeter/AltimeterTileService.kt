package ru.altimeter

import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import java.util.Locale

class AltimeterTileService : Material3TileService() {
    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        // A short, one-shot sensor read only while the system requests this tile.
        val pressure = AltimeterData.readPressure(this@AltimeterTileService, 1700L)
        val reading = pressure?.let {
            AltimeterData.getReading(this@AltimeterTileService, it)
        }

        val layout = primaryLayout(mainSlot = {
            if (reading == null) {
                text(
                    (if (pressure == null) "Барометр недоступен" else "Открой Altimeter для калибровки").layoutString,
                    typography = Typography.BODY_MEDIUM
                )
            } else {
                text(
                    String.format(Locale.getDefault(), "%.1f м", reading.altitudeM).layoutString,
                    typography = Typography.TITLE_LARGE
                )
                text(
                    String.format(Locale.getDefault(), "Ground %.1f hPa", reading.groundHpa).layoutString,
                    typography = Typography.BODY_MEDIUM
                )
                text(
                    String.format(Locale.getDefault(), "MSL %.1f hPa", reading.seaLevelHpa).layoutString,
                    typography = Typography.BODY_MEDIUM
                )
            }
        })
        return Tile.Builder()
            .setResourcesVersion("1")
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
    }
}
