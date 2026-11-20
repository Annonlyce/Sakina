package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ShareCardGenerator {

    suspend fun shareSurahCard(
        context: Context,
        surahNumber: Int,
        reciterName: String,
        moshafName: String
    ) {
        val shareFile = withContext(Dispatchers.IO) {
            val bitmap = renderCardBitmap(context, surahNumber, reciterName, moshafName)
            val dir = File(context.cacheDir, "shares")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "sakina_share_${surahNumber}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            file
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )

        val surahName = SurahMetadata.byNumber(surahNumber).name
        val shareText = "أستمع الآن إلى سورة $surahName بصوت القارئ $reciterName عبر تطبيق سكينة"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "مشاركة التلاوة")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun renderCardBitmap(
        context: Context,
        surahNumber: Int,
        reciterName: String,
        moshafName: String
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(0xFF121316.toInt(), 0xFF181B20.toInt(), 0xFF121316.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle emerald glow at top center
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                width / 2f, 340f, 460f,
                0x281FD49B, 0x001FD49B,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width / 2f, 340f, 460f, glowPaint)

        // Inner card frame
        val cardRect = RectF(72f, 72f, width - 72f, height - 72f)
        val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCC1A1C21.toInt()
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, cardFill)

        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = 0xFF2D3138.toInt()
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, cardBorder)

        // Crescent mark using drawable
        val crescentDrawable = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_sakina_crescent, null)
        crescentDrawable?.let {
            val size = 120
            val left = (width - size) / 2
            val top = 190
            it.setBounds(left, top, left + size, top + size)
            it.draw(canvas)
        }

        val cairoTypeface = ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT
        val amiriTypeface = ResourcesCompat.getFont(context, R.font.amiri_quran) ?: Typeface.SERIF

        // App name
        val appPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1FD49B.toInt()
            textSize = 46f
            typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("سكينة", width / 2f, 390f, appPaint)

        // Divider accent
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1FD49B.toInt()
            strokeWidth = 4f
            alpha = 110
        }
        canvas.drawLine(width / 2f - 80f, 445f, width / 2f + 80f, 445f, linePaint)

        // Surah info
        val surah = SurahMetadata.byNumber(surahNumber)
        val surahPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFECEEF1.toInt()
            textSize = 110f
            typeface = amiriTypeface
        }
        val surahLayout = StaticLayout.Builder
            .obtain("سورة ${surah.name}", 0, "سورة ${surah.name}".length, surahPaint, width - 220)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        canvas.save()
        canvas.translate(110f, 540f)
        surahLayout.draw(canvas)
        canvas.restore()

        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF9AA1AC.toInt()
            textSize = 38f
            typeface = cairoTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(surah.subtitleArabic(), width / 2f, 770f, subtitlePaint)

        // Reciter pill box
        val pillRect = RectF(160f, 860f, width - 160f, 1060f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF23262C.toInt()
        }
        canvas.drawRoundRect(pillRect, 36f, 36f, pillPaint)

        val reciterPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFECEEF1.toInt()
            textSize = 46f
            typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(reciterName, width / 2f, 948f, reciterPaint)

        val moshafPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1FD49B.toInt()
            textSize = 34f
            typeface = cairoTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(moshafName, width / 2f, 1012f, moshafPaint)

        // Footer
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF9AA1AC.toInt()
            textSize = 32f
            typeface = cairoTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("القرآن الكريم · سكينة وطمأنينة", width / 2f, 1210f, footerPaint)

        return bitmap
    }
}
