package com.cardinalich.kardinalgrab

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class SplashActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var opened = false
    private val opener = Runnable { openMain() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(Color.WHITE)
            setOnClickListener { openMain() }
        }
        root.addView(ImageView(this).apply {
            setImageResource(com.cardinalich.kardinalgrab.R.drawable.splash_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "Cardinalich Software"
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply {
            text = "КАРДИНАЛЬНЫЙ ХВАТ"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(CARDINAL_RED)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        })
        root.addView(TextView(this).apply {
            text = "Хвать ссылку — тащи видео"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(INK)
            setPadding(0, dp(7), 0, dp(20))
        })
        root.addView(TextView(this).apply {
            text = "Авторская разработка Cardinalich Software\n© 2026"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(MUTED)
        })
        setContentView(root)

        val fastStart = intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_VIEW
        handler.postDelayed(opener, if (fastStart) 220L else 1800L)
    }

    private fun openMain() {
        if (opened) return
        opened = true
        handler.removeCallbacks(opener)
        val source = intent
        val next = Intent(this, MainActivity::class.java).apply {
            action = source.action
            type = source.type
            data = source.data
            source.extras?.let { putExtras(it) }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(next)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(opener)
        super.onDestroy()
    }
}
