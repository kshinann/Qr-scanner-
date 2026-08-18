package com.qrtoolkit.app.data.generate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.qrtoolkit.app.data.model.QrDotStyle
import com.qrtoolkit.app.data.model.QrStyleOptions

/**
 * Renders a QR code as a styled [Bitmap] -- colors, square-vs-rounded modules, and an optional
 * center logo -- rather than ZXing's default plain black-square renderer. When a logo is
 * present the error-correction level is bumped to H (~30% recoverable) since the logo occludes
 * real modules; anything less and a logo-bearing code can become unscannable.
 */
object QrEncoder {

    fun encode(
        content: String,
        sizePx: Int,
        style: QrStyleOptions = QrStyleOptions.DEFAULT,
        logo: Bitmap? = null,
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to if (logo != null) ErrorCorrectionLevel.H else ErrorCorrectionLevel.M,
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val rendered = renderMatrix(matrix, sizePx, style)
        return if (logo != null) overlayLogo(rendered, logo) else rendered
    }

    private fun renderMatrix(matrix: BitMatrix, sizePx: Int, style: QrStyleOptions): Bitmap {
        val moduleCountX = matrix.width
        val moduleCountY = matrix.height
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(style.backgroundColorArgb)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.foregroundColorArgb }
        val moduleWidth = sizePx.toFloat() / moduleCountX
        val moduleHeight = sizePx.toFloat() / moduleCountY

        for (y in 0 until moduleCountY) {
            for (x in 0 until moduleCountX) {
                if (!matrix[x, y]) continue
                val left = x * moduleWidth
                val top = y * moduleHeight
                when (style.dotStyle) {
                    QrDotStyle.SQUARE -> canvas.drawRect(left, top, left + moduleWidth, top + moduleHeight, paint)
                    QrDotStyle.ROUNDED -> {
                        val radius = minOf(moduleWidth, moduleHeight) / 2f * 0.9f
                        canvas.drawCircle(left + moduleWidth / 2f, top + moduleHeight / 2f, radius, paint)
                    }
                }
            }
        }
        return bitmap
    }

    private fun overlayLogo(qrBitmap: Bitmap, logo: Bitmap): Bitmap {
        val result = qrBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val logoSize = (result.width * LOGO_SIZE_FRACTION).toInt()
        val scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
        val left = (result.width - logoSize) / 2f
        val top = (result.height - logoSize) / 2f

        // White backing plate so the logo stays legible against whatever modules sit behind it.
        val backingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(
            left + logoSize / 2f,
            top + logoSize / 2f,
            logoSize / 2f * 1.15f,
            backingPaint,
        )
        canvas.drawBitmap(scaledLogo, left, top, null)
        return result
    }

    // Kept well under error-correction-H's ~30% recovery budget once the round backing plate
    // (a bit larger than the logo itself) is accounted for.
    private const val LOGO_SIZE_FRACTION = 0.20f
}
