package com.cardinalich.kardinalgrab

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class LoginActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var done: Button
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        configureWebView()
        webView.loadUrl("https://www.instagram.com/accounts/login/")
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(12), dp(15), dp(9))
            setBackgroundColor(PAPER)
        }
        header.addView(TextView(this).apply {
            text = "Внутренний вход в Instagram"
            textSize = 21f
            setTextColor(ULTRAMARINE_DARK)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        })
        status = TextView(this).apply {
            text = "Войдите в аккаунт. Логин и пароль получает только Instagram."
            textSize = 13f
            setTextColor(MUTED)
            setPadding(0, dp(4), 0, 0)
        }
        header.addView(status)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.GONE
        }
        header.addView(progress, LinearLayout.LayoutParams(-1, dp(3)).apply { topMargin = dp(7) })
        root.addView(header)

        webView = WebView(this)
        root.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))

        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(7), dp(9), 0)
            setBackgroundColor(Color.WHITE)
        }
        navigation.addView(secondaryButton("Назад") {
            if (webView.canGoBack()) webView.goBack()
        }, LinearLayout.LayoutParams(0, dp(45), 1f).apply { marginEnd = dp(4) })
        navigation.addView(secondaryButton("Обновить") { webView.reload() },
            LinearLayout.LayoutParams(0, dp(45), 1f).apply { marginStart = dp(4) })
        root.addView(navigation)

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(8), dp(9), dp(11))
            setBackgroundColor(Color.WHITE)
        }
        footer.addView(secondaryButton("Отмена") { finish() },
            LinearLayout.LayoutParams(0, dp(51), 1f).apply { marginEnd = dp(5) })
        done = primaryButton("Готово") {
            CookieManager.getInstance().flush()
            if (hasSession()) {
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Сначала завершите вход в Instagram", Toast.LENGTH_SHORT).show()
            }
        }.apply { isEnabled = false }
        footer.addView(done, LinearLayout.LayoutParams(0, dp(51), 1f).apply { marginStart = dp(5) })
        root.addView(footer)

        root.addView(TextView(this).apply {
            text = "Cardinalich Software • сессия остаётся только внутри приложения"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(MUTED)
            setPadding(0, 0, 0, dp(5))
        })
        return root
    }

    private fun configureWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowContentAccess = true
            allowFileAccess = false
            mediaPlaybackRequiresUserGesture = true
            builtInZoomControls = false
            displayZoomControls = false
            userAgentString = WebSettings.getDefaultUserAgent(this@LoginActivity)
                .replace("; wv", "")
                .replace("Version/4.0 ", "")
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url.isNullOrBlank()) return false
                val uri = Uri.parse(url)
                return if (uri.scheme == "http" || uri.scheme == "https") {
                    false
                } else {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: Exception) {
                        // Instagram иногда предлагает открыть своё приложение. Просто остаёмся в WebView.
                    }
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                cookieManager.flush()
                updateState()
            }
        }
    }

    private fun updateState() {
        val ok = hasSession()
        done.isEnabled = ok
        status.text = if (ok) {
            "● Вход выполнен. Нажмите «Готово»."
        } else {
            "Войдите в аккаунт. Логин и пароль получает только Instagram."
        }
        status.setTextColor(if (ok) GREEN else MUTED)
    }

    private fun hasSession(): Boolean {
        val cookies = CookieManager.getInstance().getCookie("https://www.instagram.com/").orEmpty()
        return cookies.split(';').any {
            val value = it.trim()
            value.startsWith("sessionid=") && value.substringAfter('=').isNotBlank()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
        super.onDestroy()
    }
}
