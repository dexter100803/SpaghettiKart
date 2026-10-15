package com.izzy.kart

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.lang.ref.WeakReference

/**
 * Dark palette with a selectable accent color, shared by the launcher, the mod
 * manager and the saves screens. The name is historical: gold is the default.
 */
object GoldTheme {

    enum class Preset(val label: String, val color: Int) {
        GOLD("Gold", Color.rgb(212, 175, 55)),
        RED("Red", Color.rgb(214, 56, 56)),
        BLUE("Blue", Color.rgb(64, 130, 235)),
        GREEN("Green", Color.rgb(60, 180, 90)),
        PURPLE("Purple", Color.rgb(150, 90, 220)),
        ORANGE("Orange", Color.rgb(235, 135, 40)),
        SILVER("Silver", Color.rgb(190, 196, 206)),
        RGB("RGB", Color.rgb(214, 56, 56))
    }

    private const val PREFS = "kart_theme"
    private const val KEY_PRESET = "preset"

    private var preset = Preset.GOLD
    private var accent = Preset.GOLD.color
    private var lastDensity = 2f

    val BLACK = Color.rgb(10, 9, 8)
    val TEXT = Color.rgb(244, 238, 226)

    val BLACK_SOFT: Int get() = blend(BLACK, accent, 0.14f)
    val GOLD: Int get() = accent
    val GOLD_LIGHT: Int get() = blend(accent, Color.WHITE, 0.45f)
    val GOLD_DARK: Int get() = blend(accent, Color.BLACK, 0.40f)
    val SUBTLE: Int get() = blend(Color.rgb(160, 160, 160), accent, 0.35f)
    val ROW_EVEN: Int get() = Color.argb(28, Color.red(accent), Color.green(accent), Color.blue(accent))

    private fun blend(from: Int, to: Int, t: Float): Int {
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
        return Color.rgb(r, g, b)
    }

    /** Reads the saved accent choice; call at the start of every screen. */
    fun load(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PRESET, null)
        preset = Preset.values().firstOrNull { it.name == saved } ?: Preset.GOLD
        accent = preset.color
    }

    fun select(context: Context, selected: Preset) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PRESET, selected.name).apply()
        preset = selected
        accent = selected.color
    }

    /** Dark screen background with a faint glow of the accent color at the top. */
    fun background() = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(BLACK_SOFT, BLACK)
    )

    /** Accent gradient button with dark text; dims when disabled. */
    fun styleButton(button: Button, density: Float) {
        lastDensity = density
        applyButton(button)
        buttons.add(WeakReference(button))
    }

    private fun applyButton(button: Button) {
        val radius = 10f * lastDensity
        val enabled = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(GOLD_LIGHT, GOLD, GOLD_DARK)
        ).apply { cornerRadius = radius }
        val disabled = GradientDrawable().apply {
            setColor(Color.argb(60, Color.red(accent), Color.green(accent), Color.blue(accent)))
            cornerRadius = radius
        }
        button.background = StateListDrawable().apply {
            addState(intArrayOf(-android.R.attr.state_enabled), disabled)
            addState(intArrayOf(), enabled)
        }
        button.setTextColor(
            ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                intArrayOf(SUBTLE, BLACK)
            )
        )
        button.stateListAnimator = null
    }

    fun tint(progress: ProgressBar) {
        progress.indeterminateTintList = ColorStateList.valueOf(GOLD)
    }

    /** Registers a title so it follows the accent color (needed for the RGB preset). */
    fun trackTitle(title: TextView) {
        titles.add(WeakReference(title))
    }

    /** A row of color dots; tapping one saves the choice and rebuilds the screen. */
    fun buildPicker(activity: Activity, canChange: () -> Boolean = { true }): LinearLayout {
        val density = activity.resources.displayMetrics.density
        val size = (28 * density).toInt()
        val gap = (5 * density).toInt()
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        for (option in Preset.values()) {
            val selected = option == preset
            val strokeWidth = ((if (selected) 3f else 1f) * density).toInt()
            val strokeColor = if (selected) Color.WHITE else Color.argb(90, 255, 255, 255)
            val dot = View(activity)
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.OVAL
            if (option == Preset.RGB) {
                shape.gradientType = GradientDrawable.SWEEP_GRADIENT
                shape.colors = intArrayOf(
                    Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
                )
            } else {
                shape.setColor(option.color)
            }
            shape.setStroke(strokeWidth, strokeColor)
            dot.background = shape
            dot.contentDescription = option.label
            dot.setOnClickListener {
                if (canChange()) {
                    select(activity, option)
                    activity.recreate()
                }
            }
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(gap, gap, gap, gap)
            row.addView(dot, params)
        }
        return row
    }

    // --- RGB mode: cycle the accent color while a themed screen is visible. ---

    private val handler = Handler(Looper.getMainLooper())
    private val buttons = mutableListOf<WeakReference<Button>>()
    private val titles = mutableListOf<WeakReference<TextView>>()
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running || preset != Preset.RGB) {
                running = false
                return
            }
            val hue = (SystemClock.uptimeMillis() / 25 % 360).toFloat()
            accent = Color.HSVToColor(floatArrayOf(hue, 0.80f, 0.95f))
            buttons.removeAll { it.get() == null }
            titles.removeAll { it.get() == null }
            for (ref in buttons) {
                ref.get()?.let { applyButton(it) }
            }
            for (ref in titles) {
                ref.get()?.setTextColor(accent)
            }
            handler.postDelayed(this, 60L)
        }
    }

    fun start() {
        if (preset == Preset.RGB && !running) {
            running = true
            handler.post(tick)
        }
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
    }
}
