package ru.altimeter

import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.loadAction
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import java.util.Locale

class AltimeterTileService : Material3TileService() {
    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val clickId = requestParams.currentState.lastClickableId
        val pressure = AltimeterData.readPressure(this@AltimeterTileService, 1700L)

        if (pressure != null && (clickId == "up" || clickId == "down")) {
            val current = AltimeterData.getReading(this@AltimeterTileService, pressure)
            val currentAltitude = current?.altitudeM ?: 0.0
            val delta = if (clickId == "up") 1.0 else -1.0
            AltimeterData.calibrate(this@AltimeterTileService, pressure, currentAltitude + delta)
        }

        val reading = pressure?.let {
            AltimeterData.getReading(this@AltimeterTileService, it)
        }

        val content = Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .apply {
                if (reading == null) {
                    addContent(text(
                        (if (pressure == null) "Барометр недоступен" else "Открой Altimeter для калибровки").layoutString,
                        typography = Typography.BODY_MEDIUM
                    ))
                } else {
                    addContent(text(
                        String.format(Locale.getDefault(), "%.1f м", reading.altitudeM).layoutString,
                        typography = Typography.TITLE_LARGE
                    ))
                    addContent(text(
                        String.format(Locale.getDefault(), "Ground %.1f hPa", reading.groundHpa).layoutString,
                        typography = Typography.BODY_MEDIUM
                    ))
                    addContent(text(
                        String.format(Locale.getDefault(), "MSL %.1f hPa", reading.seaLevelHpa).layoutString,
                        typography = Typography.BODY_MEDIUM
                    ))
                    addContent(
                        Row.Builder()
                            .setWidth(expand())
                            .addContent(textButton(
                                onClick = clickable(id = "down", action = loadAction()),
                                width = weight(1f),
                                labelContent = { text("▼".layoutString) }
                            ))
                            .addContent(textButton(
                                onClick = clickable(id = "up", action = loadAction()),
                                width = weight(1f),
                                labelContent = { text("▲".layoutString) }
                            ))
                            .build()
                    )
                    addContent(textButton(
                        onClick = clickable(id = "refresh", action = loadAction()),
                        width = expand(),
                        labelContent = { text("Обновить".layoutString) }
                    ))
                }
            }
            .build()

        val layout = primaryLayout(mainSlot = { content })
        return Tile.Builder()
            .setResourcesVersion("2")
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
    }
}
