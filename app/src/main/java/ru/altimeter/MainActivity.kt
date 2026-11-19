package ru.altimeter

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.wear.tiles.TileService
import java.util.Locale

class MainActivity : Activity() {
    private val scope = MainScope()
    private lateinit var status: TextView
    private lateinit var pressure: TextView
    private lateinit var seaLevel: TextView
    private lateinit var altitudeInput: EditText
    private var lastPressure: Float? = null
    private var updatingField = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refresh()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
        }
        status = TextView(this).apply {
            text = "Получение показаний…"
            textSize = 11f
            gravity = Gravity.CENTER
        }
        altitudeInput = EditText(this).apply {
            hint = "—"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            imeOptions = EditorInfo.IME_ACTION_DONE
            textSize = 40f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0)
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                    applyTypedAltitude()
                    true
                } else false
            }
        }
        pressure = TextView(this).apply {
            text = "Ground: — hPa"
            textSize = 14f
            gravity = Gravity.CENTER
        }
        seaLevel = TextView(this).apply {
            text = "MSL: — hPa"
            textSize = 14f
            gravity = Gravity.CENTER
        }

        val arrows = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        arrows.addView(button("▼", 18f) { adjust(-1.0) },
            LinearLayout.LayoutParams(0, 44, 1f))
        arrows.addView(button("▲", 18f) { adjust(1.0) },
            LinearLayout.LayoutParams(0, 44, 1f))

        root.addView(status, match())
        root.addView(altitudeInput, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(pressure, match())
        root.addView(seaLevel, match())
        root.addView(arrows, match())
        root.addView(button("Обновить", 12f) { refresh() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40))
        setContentView(root)
    }

    private fun button(caption: String, size: Float = 13f, action: () -> Unit) =
        Button(this).apply {
            text = caption
            textSize = size
            minHeight = 0
            minimumHeight = 0
            setOnClickListener { action() }
        }

    private fun match() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun typedAltitude(): Double? = altitudeInput.text.toString()
        .trim()
        .replace(',', '.')
        .toDoubleOrNull()

    private fun applyTypedAltitude() {
        val value = typedAltitude()
        if (value == null) {
            status.text = "Введи высоту числом"
            return
        }
        setCalibration(value, "Высота установлена")
    }

    private fun adjust(delta: Double) {
        val base = typedAltitude()
            ?: AltimeterData.getReading(this, lastPressure ?: return)?.altitudeM
            ?: 0.0
        setCalibration(base + delta, "Калибровка изменена")
    }

    private fun setCalibration(value: Double, successMessage: String) {
        status.text = "Считываю давление…"
        scope.launch {
            val p = AltimeterData.readPressure(this@MainActivity)
            if (p == null) {
                status.text = "Барометр не ответил. Повтори."
                return@launch
            }
            lastPressure = p
            AltimeterData.calibrate(this@MainActivity, p, value)
            TileService.getUpdater(this@MainActivity)
                .requestUpdate(AltimeterTileService::class.java)
            render(p)
            status.text = successMessage
        }
    }

    private fun refresh() {
        status.text = "Обновление…"
        scope.launch {
            val p = AltimeterData.readPressure(this@MainActivity)
            if (p == null) {
                status.text = "Барометр недоступен"
                return@launch
            }
            lastPressure = p
            if (!AltimeterData.hasCalibration(this@MainActivity)) {
                AltimeterData.calibrate(this@MainActivity, p, 0.0)
            }
            render(p)
            status.text = "Показания обновлены"
        }
    }

    private fun render(p: Float) {
        val r = AltimeterData.getReading(this, p) ?: return
        val displayedAltitude = String.format(Locale.US, "%.1f", r.altitudeM)
        if (altitudeInput.text.toString() != displayedAltitude) {
            updatingField = true
            altitudeInput.setText(displayedAltitude)
            altitudeInput.setSelection(altitudeInput.text.length)
            updatingField = false
        }
        pressure.text = String.format(Locale.getDefault(), "Ground: %.1f hPa", r.groundHpa)
        seaLevel.text = String.format(Locale.getDefault(), "MSL: %.1f hPa", r.seaLevelHpa)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
