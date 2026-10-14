package com.izzy.kart

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.Button
import android.widget.ProgressBar

/** Black and gold palette shared by the launcher, mod manager and saves screens. */
object GoldTheme {
    val BLACK = Color.rgb(10, 9, 8)
    val BLACK_SOFT = Color.rgb(28, 22, 12)
    val GOLD = Color.rgb(212, 175, 55)
    val GOLD_LIGHT = Color.rgb(246, 226, 142)
    val GOLD_DARK = Color.rgb(143, 111, 28)
    val TEXT = Color.rgb(244, 236, 214)
    val SUBTLE = Color.rgb(166, 150, 105)
    val ROW_EVEN = Color.argb(28, 212, 175, 55)

    /** Dark screen background with a faint warm glow at the top. */
    fun background() = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(BLACK_SOFT, BLACK)
    )

    /** Gold gradient button with dark text; dims when disabled. */
    fun styleButton(button: Button, density: Float) {
        val radius = 10f * density
        val enabled = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(GOLD_LIGHT, GOLD, GOLD_DARK)
        ).apply { cornerRadius = radius }
        val disabled = GradientDrawable().apply {
            setColor(Color.argb(60, 212, 175, 55))
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
}
