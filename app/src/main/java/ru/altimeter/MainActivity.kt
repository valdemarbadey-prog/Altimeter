package ru.altimeter

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : Activity() {
    private val scope = MainScope()
    private lateinit var status: TextView
    private lateinit var height: TextView
    private lateinit var pressure: TextView
    private lateinit var seaLevel: TextView
    private lateinit var altitudeInput: EditText
    private var lastPressure: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refresh()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(22, 18, 22, 18)
        }
        fun label(value: String, size: Float) = TextView(this).apply {
            text = value; textSize = size; gravity = Gravity.CENTER
            setPadding(0, 5, 0, 5)
        }
        status = label("Получение данных барометра…", 13f)
        height = label("— м", 34f)
        pressure = label("Ground: — hPa", 15f)
        seaLevel = label("MSL: — hPa", 15f)
        altitudeInput = EditText(this).apply {
            hint = "Известная высота, м"; inputType = 8194
            textSize = 16f; gravity = Gravity.CENTER
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        row.addView(button("−1 м") { adjust(-1.0) }, LinearLayout.LayoutParams(0, 52, 1f))
        row.addView(button("+1 м") { adjust(1.0) }, LinearLayout.LayoutParams(0, 52, 1f))
        val calibrate = button("Установить высоту") {
            val value = altitudeInput.text.toString().replace(',', '.').toDoubleOrNull()
            val p = lastPressure
            if (value == null) status.text = "Введи высоту числом"
            else if (p == null) status.text = "Нет данных барометра. Обнови показания."
            else {
                AltimeterData.calibrate(this, p, value)
                status.text = "Калибровка сохранена"
                render(p)
            }
        }
        root.addView(status, match())
        root.addView(height, match())
        root.addView(pressure, match())
        root.addView(seaLevel, match())
        root.addView(row, match())
        root.addView(altitudeInput, match())
        root.addView(calibrate, match())
        root.addView(button("Обновить") { refresh() }, match())
        setContentView(root)
    }

    private fun button(caption: String, action: () -> Unit) = Button(this).apply {
        text = caption; textSize = 13f; setOnClickListener { action() }
    }

    private fun match() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun adjust(delta: Double) {
        val p = lastPressure
        if (p == null) { status.text = "Сначала получи показания барометра"; return }
        val old = AltimeterData.getReading(this, p)?.altitudeM ?: 0.0
        AltimeterData.calibrate(this, p, old + delta)
        status.text = "Калибровка скорректирована"
        render(p)
    }

    private fun refresh() {
        status.text = "Считываю барометр…"
        scope.launch {
            val p = AltimeterData.readPressure(this@MainActivity)
            if (p == null) { status.text = "Барометр недоступен или не ответил"; return@launch }
            lastPressure = p
            if (!AltimeterData.hasCalibration(this@MainActivity)) {
                AltimeterData.calibrate(this@MainActivity, p, 0.0)
                status.text = "Начальная точка 0 м. Укажи известную высоту."
            } else status.text = "Показания обновлены"
            render(p)
        }
    }

    private fun render(p: Float) {
        val r = AltimeterData.getReading(this, p) ?: return
        height.text = String.format(Locale.getDefault(), "%.1f м", r.altitudeM)
        pressure.text = String.format(Locale.getDefault(), "Ground: %.1f hPa", r.groundHpa)
        seaLevel.text = String.format(Locale.getDefault(), "MSL: %.1f hPa", r.seaLevelHpa)
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
