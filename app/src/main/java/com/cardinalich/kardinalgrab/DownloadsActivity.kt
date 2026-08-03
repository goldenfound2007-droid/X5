package com.cardinalich.kardinalgrab

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

class DownloadsActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var listBox: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var clearAllButton: Button
    private val items = mutableListOf<DownloadedItem>()
    private val folderPath = Environment.DIRECTORY_DOWNLOADS + "/Кардинальный Хват/"

    data class DownloadedItem(
        val uri: Uri,
        val name: String,
        val mime: String,
        val size: Long,
        val dateAddedSeconds: Long
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        loadFiles()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(PAPER)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(12))
            setBackgroundColor(ULTRAMARINE_DARK)
        }
        header.addView(TextView(this).apply {
            text = "МОЯ ДОБЫЧА"
            textSize = 24f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        })
        header.addView(TextView(this).apply {
            text = "Открыть, отправить, удалить — всё под контролем"
            textSize = 13f
            setTextColor(Color.rgb(225, 228, 250))
            setPadding(0, dp(4), 0, 0)
        })
        root.addView(header)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        controls.addView(secondaryButton("Назад") { finish() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(5) })
        clearAllButton = dangerButton("Удалить всё") { confirmDeleteAll() }
        controls.addView(clearAllButton,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(5) })
        root.addView(controls)

        progress = ProgressBar(this).apply {
            isIndeterminate = true
            visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(38)).apply {
            marginStart = dp(20)
            marginEnd = dp(20)
        })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        listBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(2), dp(12), dp(22))
        }
        emptyText = TextView(this).apply {
            text = "Пока пусто. Кардинальный Хват ещё ничего не утащил."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(MUTED)
            setPadding(dp(20), dp(70), dp(20), dp(30))
        }
        listBox.addView(emptyText)
        scroll.addView(listBox)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(TextView(this).apply {
            text = "CARDINALICH SOFTWARE • © 2026"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(MUTED)
            setPadding(0, dp(7), 0, dp(9))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        })
        return root
    }

    private fun loadFiles() {
        progress.visibility = View.VISIBLE
        clearAllButton.isEnabled = false
        worker.execute {
            val loaded = mutableListOf<DownloadedItem>()
            try {
                val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.MIME_TYPE,
                    MediaStore.Downloads.SIZE,
                    MediaStore.Downloads.DATE_ADDED
                )
                val selection = "${MediaStore.Downloads.RELATIVE_PATH}=?"
                val selectionArgs = arrayOf(folderPath)
                contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.Downloads.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)
                    val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                    val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        loaded += DownloadedItem(
                            uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id),
                            name = cursor.getString(nameIndex) ?: "Видео Instagram",
                            mime = cursor.getString(mimeIndex) ?: "video/mp4",
                            size = cursor.getLong(sizeIndex),
                            dateAddedSeconds = cursor.getLong(dateIndex)
                        )
                    }
                }
            } catch (_: Exception) {
                // Ошибка будет отражена пустым списком, чтобы экран не падал.
            }
            runOnUiThread {
                items.clear()
                items.addAll(loaded)
                renderItems()
                progress.visibility = View.GONE
                clearAllButton.isEnabled = items.isNotEmpty()
            }
        }
    }

    private fun renderItems() {
        listBox.removeAllViews()
        if (items.isEmpty()) {
            emptyText = TextView(this).apply {
                text = "Пока пусто. Кардинальный Хват ещё ничего не утащил."
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(MUTED)
                setPadding(dp(20), dp(70), dp(20), dp(30))
            }
            listBox.addView(emptyText)
            return
        }
        items.forEachIndexed { index, item ->
            listBox.addView(buildItemCard(item), LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = if (index == 0) 0 else dp(10)
            })
        }
    }

    private fun buildItemCard(item: DownloadedItem): View {
        val card = card().apply {
            setOnClickListener { open(item) }
        }
        card.addView(TextView(this).apply {
            text = item.name
            textSize = 15f
            setTextColor(INK)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 2
        })
        val dateText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(item.dateAddedSeconds * 1000L))
        card.addView(TextView(this).apply {
            text = "$dateText  •  ${Formatter.formatFileSize(this@DownloadsActivity, item.size)}"
            textSize = 12f
            setTextColor(MUTED)
            setPadding(0, dp(5), 0, dp(10))
        })

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(secondaryButton("Открыть") { open(item) },
            LinearLayout.LayoutParams(0, dp(45), 1f).apply { marginEnd = dp(3) })
        row.addView(primaryButton("Поделиться") { share(item) },
            LinearLayout.LayoutParams(0, dp(45), 1.25f).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            })
        row.addView(dangerButton("Удалить") { confirmDelete(item) },
            LinearLayout.LayoutParams(0, dp(45), 1f).apply { marginStart = dp(3) })
        card.addView(row)
        return card
    }

    private fun open(item: DownloadedItem) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.uri, item.mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (_: Exception) {
            toast("На телефоне нет приложения для открытия файла")
        }
    }

    private fun share(item: DownloadedItem) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = item.mime
            putExtra(Intent.EXTRA_STREAM, item.uri)
            putExtra(Intent.EXTRA_TEXT, "Скачано приложением «Кардинальный Хват» — Cardinalich Software")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "WhatsApp, MAX или другое приложение"))
    }

    private fun confirmDelete(item: DownloadedItem) {
        AlertDialog.Builder(this)
            .setTitle("Удалить файл?")
            .setMessage(item.name)
            .setNegativeButton("Оставить", null)
            .setPositiveButton("Удалить") { _, _ ->
                try {
                    contentResolver.delete(item.uri, null, null)
                    toast("Файл удалён")
                    loadFiles()
                } catch (e: Exception) {
                    toast("Не удалось удалить: ${e.message.orEmpty().take(100)}")
                }
            }
            .show()
    }

    private fun confirmDeleteAll() {
        if (items.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("Удалить всю добычу?")
            .setMessage("Будут удалены ${items.size} файл(а/ов) из папки «Кардинальный Хват». Отменить это нельзя.")
            .setNegativeButton("Нет", null)
            .setPositiveButton("Удалить всё") { _, _ ->
                worker.execute {
                    var removed = 0
                    items.toList().forEach {
                        try {
                            removed += contentResolver.delete(it.uri, null, null)
                        } catch (_: Exception) {
                        }
                    }
                    runOnUiThread {
                        toast("Удалено файлов: $removed")
                        loadFiles()
                    }
                }
            }
            .show()
    }

    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
