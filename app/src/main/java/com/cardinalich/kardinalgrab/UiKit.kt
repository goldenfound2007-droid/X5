package com.cardinalich.kardinalgrab

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

val ULTRAMARINE: Int = Color.rgb(47, 70, 211)
val ULTRAMARINE_DARK: Int = Color.rgb(24, 38, 154)
val CARDINAL_RED: Int = Color.rgb(178, 11, 22)
val INK: Int = Color.rgb(17, 19, 26)
val MUTED: Int = Color.rgb(96, 101, 119)
val PAPER: Int = Color.rgb(245, 246, 250)
val GREEN: Int = Color.rgb(18, 137, 73)

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun Context.rounded(fill: Int, stroke: Int, radius: Int, strokeDp: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        if (strokeDp > 0) setStroke(dp(strokeDp), stroke)
    }

fun Activity.card(top: Int = 0): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(18), dp(18), dp(18), dp(18))
    background = rounded(Color.WHITE, Color.rgb(224, 227, 238), 20, 1)
    elevation = dp(3).toFloat()
    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }
}

fun Activity.sectionLabel(value: String): TextView = TextView(this).apply {
    text = value
    textSize = 12f
    setTextColor(ULTRAMARINE_DARK)
    setTypeface(Typeface.DEFAULT, Typeface.BOLD)
    letterSpacing = 0.08f
}

fun Activity.primaryButton(value: String, click: () -> Unit): Button = Button(this).apply {
    text = value
    textSize = 14f
    setTextColor(Color.WHITE)
    setTypeface(Typeface.DEFAULT, Typeface.BOLD)
    isAllCaps = false
    background = rounded(ULTRAMARINE, ULTRAMARINE, 15)
    setOnClickListener { click() }
}

fun Activity.secondaryButton(value: String, click: () -> Unit): Button = Button(this).apply {
    text = value
    textSize = 14f
    setTextColor(ULTRAMARINE_DARK)
    setTypeface(Typeface.DEFAULT, Typeface.BOLD)
    isAllCaps = false
    background = rounded(Color.WHITE, Color.rgb(183, 190, 224), 15, 1)
    setOnClickListener { click() }
}

fun Activity.dangerButton(value: String, click: () -> Unit): Button = Button(this).apply {
    text = value
    textSize = 14f
    setTextColor(Color.WHITE)
    setTypeface(Typeface.DEFAULT, Typeface.BOLD)
    isAllCaps = false
    background = rounded(CARDINAL_RED, CARDINAL_RED, 15)
    setOnClickListener { click() }
}
