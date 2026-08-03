package com.cardinalich.kardinalgrab

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.chaquo.python.Python
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var authStatus: TextView
    private lateinit var linkInput: EditText
    private lateinit var downloadButton: Button
    private lateinit var progress: ProgressBar
    private lateinit var message: TextView
    private lateinit var openButton: Button
    private lateinit var shareButton: Button
    private var lastSavedUri: Uri? = null
    private var lastMime = "video/mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshAuthStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(PAPER)
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(26))
        }
        scroll.addView(root)

        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.splash_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "Cardinalich Software"
        }, LinearLayout.LayoutParams(-1, dp(150)))

        root.addView(TextView(this).apply {
            text = "КАРДИНАЛЬНЫЙ ХВАТ"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(CARDINAL_RED)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.05f
        })
        root.addView(TextView(this).apply {
            text = "Ссылка попалась — видео не уйдёт"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(MUTED)
            setPadding(0, dp(5), 0, dp(18))
        })

        val authCard = card()
        authCard.addView(sectionLabel("ВНУТРЕННЯЯ АВТОРИЗАЦИЯ INSTAGRAM"))
        authStatus = TextView(this).apply {
            textSize = 16f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, dp(10), 0, dp(12))
        }
        authCard.addView(authStatus)
        authCard.addView(primaryButton("Войти в Instagram внутри приложения") {
            startActivity(Intent(this, LoginActivity::class.java))
        })
        authCard.addView(secondaryButton("Выйти и очистить сессию") {
            confirmLogout()
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })
        root.addView(authCard)

        val downloadCard = card(top = 14)
        downloadCard.addView(sectionLabel("ССЫЛКА НА REELS ИЛИ ПУБЛИКАЦИЮ"))
        linkInput = EditText(this).apply {
            hint = "Ссылка подставится сама через «Поделиться»"
            textSize = 16f
            setTextColor(INK)
            setHintTextColor(Color.rgb(142, 147, 163))
            minLines = 2
            maxLines = 4
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.WHITE, Color.rgb(210, 214, 229), 15, 1)
        }
        downloadCard.addView(linkInput, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        val utilityRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        utilityRow.addView(secondaryButton("Вставить") { pasteFromClipboard() },
            LinearLayout.LayoutParams(0, dp(49), 1f).apply { marginEnd = dp(5) })
        utilityRow.addView(secondaryButton("Очистить") {
            linkInput.text.clear()
            message.text = "В Instagram нажми «Поделиться» и выбери «Кардинальный Хват»."
            message.setTextColor(MUTED)
        }, LinearLayout.LayoutParams(0, dp(49), 1f).apply { marginStart = dp(5) })
        downloadCard.addView(utilityRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })

        downloadButton = primaryButton("СКАЧАТЬ НА ТЕЛЕФОН") { startDownload() }
        downloadCard.addView(downloadButton, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(11) })

        progress = ProgressBar(this).apply {
            isIndeterminate = true
            visibility = View.GONE
        }
        downloadCard.addView(progress, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(7) })

        message = TextView(this).apply {
            text = "В Instagram нажми «Поделиться» и выбери «Кардинальный Хват». Ссылка появится здесь сама."
            textSize = 14f
            setTextColor(MUTED)
            setPadding(0, dp(10), 0, 0)
        }
        downloadCard.addView(message)

        openButton = secondaryButton("Открыть скачанный файл") { openLast() }.apply { visibility = View.GONE }
        downloadCard.addView(openButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(11) })
        shareButton = secondaryButton("Поделиться: WhatsApp, MAX и другие") { shareLast() }.apply { visibility = View.GONE }
        downloadCard.addView(shareButton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        root.addView(downloadCard)

        val libraryCard = card(top = 14)
        libraryCard.addView(sectionLabel("МОЯ ДОБЫЧА"))
        libraryCard.addView(TextView(this).apply {
            text = "Просматривай, отправляй и удаляй всё, что уже скачано."
            textSize = 14f
            setTextColor(MUTED)
            setPadding(0, dp(8), 0, dp(10))
        })
        libraryCard.addView(primaryButton("Открыть мои загрузки") {
            startActivity(Intent(this, DownloadsActivity::class.java))
        })
        root.addView(libraryCard)

        val bottomRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottomRow.addView(secondaryButton("О программе") { showAbout() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(5) })
        bottomRow.addView(secondaryButton("Скопировать ссылку") {
            val value = linkInput.text.toString().trim()
            if (value.isBlank()) toast("Ссылки пока нет") else {
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Instagram", value))
                toast("Ссылка скопирована")
            }
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(5) })
        root.addView(bottomRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        root.addView(TextView(this).apply {
            text = "CARDINALICH SOFTWARE\nАвторская разработка • © 2026\nСессия хранится только на этом телефоне"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(MUTED)
            setPadding(dp(8), dp(20), dp(8), 0)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.05f
        })

        refreshAuthStatus()
        return scroll
    }

    private fun refreshAuthStatus() {
        if (!::authStatus.isInitialized) return
        val ok = hasSession()
        authStatus.text = if (ok) "● Вход выполнен — Хват готов" else "● Сначала войдите в Instagram"
        authStatus.setTextColor(if (ok) GREEN else CARDINAL_RED)
    }

    private fun hasSession(): Boolean {
        val cookies = CookieManager.getInstance().getCookie("https://www.instagram.com/").orEmpty()
        return cookies.split(';').any {
            val value = it.trim()
            value.startsWith("sessionid=") && value.substringAfter('=').isNotBlank()
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val raw = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        val url = extractInstagramUrl(raw)
        if (url == null) toast("В буфере нет ссылки Instagram") else {
            linkInput.setText(url)
            linkInput.setSelection(url.length)
            message.setTextColor(GREEN)
            message.text = "Ссылка поймана. Осталось нажать «Скачать»."
        }
    }

    private fun startDownload() {
        val url = extractInstagramUrl(linkInput.text.toString())
        if (url == null) {
            showError("Нужна корректная ссылка Instagram.")
            return
        }
        val cookies = CookieManager.getInstance().getCookie("https://www.instagram.com/").orEmpty()
        if (!hasSession()) {
            showError("Сначала войдите в Instagram внутри приложения.")
            return
        }

        setBusy(true)
        message.setTextColor(MUTED)
        message.text = "Кардинальный Хват вышел на охоту…"
        val ua = WebSettings.getDefaultUserAgent(this)
        val tempDir = File(cacheDir, "cardinal_grab").apply {
            deleteRecursively()
            mkdirs()
        }

        worker.execute {
            try {
                val module = Python.getInstance().getModule("downloader")
                val raw = module.callAttr(
                    "download_instagram",
                    url,
                    cookies,
                    tempDir.absolutePath,
                    ua
                ).toString()
                val result = JSONObject(raw)
                if (!result.optBoolean("ok")) {
                    throw RuntimeException(result.optString("error", "Неизвестная ошибка загрузки"))
                }
                val source = File(result.getString("path"))
                val proposedName = result.optString("filename", source.name)
                val saved = saveToDownloads(source, proposedName)
                runOnUiThread {
                    lastSavedUri = saved.first
                    lastMime = saved.second
                    message.setTextColor(GREEN)
                    message.text = "Добыча взята. Файл лежит в Download/Кардинальный Хват."
                    openButton.visibility = View.VISIBLE
                    shareButton.visibility = View.VISIBLE
                    setBusy(false)
                    toast("Видео скачано")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showError(humanError(e.message.orEmpty()))
                    setBusy(false)
                }
            }
        }
    }

    private fun saveToDownloads(source: File, requestedName: String): Pair<Uri, String> {
        val ext = source.extension.lowercase(Locale.ROOT).ifBlank { "mp4" }
        val mime = when (ext) {
            "webm" -> "video/webm"
            "m4v" -> "video/x-m4v"
            else -> "video/mp4"
        }
        val cleanBase = requestedName.substringBeforeLast('.', requestedName)
            .replace(Regex("[\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim().take(78).ifBlank { "instagram_video" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val displayName = "${cleanBase}_$stamp.$ext"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Кардинальный Хват")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw RuntimeException("Android не смог создать файл в папке Download.")
        try {
            contentResolver.openOutputStream(uri, "w")!!.use { output ->
                FileInputStream(source).use { input -> input.copyTo(output, 256 * 1024) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            source.delete()
            return uri to mime
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
    }

    private fun openLast() {
        val uri = lastSavedUri ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, lastMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (_: Exception) {
            toast("Не найдено приложение для открытия видео")
        }
    }

    private fun shareLast() {
        val uri = lastSavedUri ?: return
        shareUri(uri, lastMime)
    }

    private fun shareUri(uri: Uri, mime: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Скачано приложением «Кардинальный Хват» — Cardinalich Software")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "Отправить видео через WhatsApp, MAX или другое приложение"))
    }

    private fun handleIntent(incoming: Intent?) {
        if (!::linkInput.isInitialized || incoming == null) return
        val raw = when (incoming.action) {
            Intent.ACTION_SEND -> incoming.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            Intent.ACTION_VIEW -> incoming.dataString.orEmpty()
            else -> ""
        }
        extractInstagramUrl(raw)?.let {
            linkInput.setText(it)
            linkInput.setSelection(it.length)
            message.setTextColor(GREEN)
            message.text = "Ссылка подхвачена из Instagram. Осталось нажать «Скачать»."
        }
    }

    private fun extractInstagramUrl(raw: String): String? {
        val regex = Regex("https?://(?:www\\.)?instagram\\.com/[^\\s<>\"']+", RegexOption.IGNORE_CASE)
        return regex.find(raw.trim())?.value?.trimEnd('.', ',', ')', ']', '}', ';')
    }

    private fun humanError(value: String): String {
        val lower = value.lowercase(Locale.ROOT)
        return when {
            "login" in lower || "session" in lower || "cookie" in lower ->
                "Instagram не принял сессию. Откройте внутренний вход, убедитесь, что видите ленту, и повторите."
            "private" in lower || "not available" in lower ->
                "Публикация недоступна текущему аккаунту. Для закрытого профиля аккаунт должен быть одобренным подписчиком."
            "unsupported" in lower -> "Эта ссылка пока не поддерживается загрузчиком."
            "network" in lower || "timed out" in lower || "connection" in lower ->
                "Ошибка сети. Проверьте интернет и повторите."
            "no video" in lower -> "В публикации не найдено видео."
            value.isBlank() -> "Не удалось скачать видео."
            else -> value.take(420)
        }
    }

    private fun setBusy(value: Boolean) {
        downloadButton.isEnabled = !value
        progress.visibility = if (value) View.VISIBLE else View.GONE
        if (value) {
            openButton.visibility = View.GONE
            shareButton.visibility = View.GONE
        }
    }

    private fun showError(value: String) {
        message.setTextColor(CARDINAL_RED)
        message.text = value
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Очистить авторизацию?")
            .setMessage("Внутренняя сессия Instagram будет удалена только с этого телефона.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Выйти") { _, _ ->
                CookieManager.getInstance().removeAllCookies {
                    CookieManager.getInstance().flush()
                    WebStorage.getInstance().deleteAllData()
                    runOnUiThread {
                        refreshAuthStatus()
                        toast("Сессия Instagram очищена")
                    }
                }
            }
            .show()
    }

    private fun showAbout() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(10), dp(20), dp(6))
        }
        box.addView(ImageView(this).apply {
            setImageResource(R.drawable.splash_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }, LinearLayout.LayoutParams(-1, dp(210)))
        box.addView(TextView(this).apply {
            text = "КАРДИНАЛЬНЫЙ ХВАТ"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(CARDINAL_RED)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        })
        box.addView(TextView(this).apply {
            text = "Версия 1.0\n\nАвтор и разработчик:\nCARDINALICH SOFTWARE\n\nХвать ссылку — тащи видео.\nСделано с характером и без зависимости от компьютера.\n\n© 2026 Cardinalich Software"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(INK)
            setPadding(0, dp(10), 0, 0)
        })
        AlertDialog.Builder(this)
            .setView(box)
            .setPositiveButton("Кардинально понятно", null)
            .show()
    }

    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
