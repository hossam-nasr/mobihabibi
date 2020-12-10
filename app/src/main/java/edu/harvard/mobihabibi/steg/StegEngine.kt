package edu.harvard.mobihabibi.steg

import android.app.Activity
import android.widget.ProgressBar
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi

class StegEngine(private val context: Context, private val progressBar: ProgressBar) {

    init {
        (context as Activity).runOnUiThread {
            progressBar.progress = 0
        }
    }

    private fun colorToRgb(color: Int): List<Int> {
        val a: Int = (color shr 24) and 0xff
        val r: Int = (color shr 16) and 0xff
        val g: Int = (color shr 8) and 0xff
        val b: Int = (color and 0xff)

        return listOf(a, r, g, b)
    }

    private fun mergeArgb(argb1: List<Int>, argb2: List<Int>): List<Int> {
        val a: Int = mergeOneNum(argb1[0], argb2[0])
        val r: Int = mergeOneNum(argb1[1], argb2[1])
        val g: Int = mergeOneNum(argb1[2], argb2[2])
        val b: Int = mergeOneNum(argb1[3], argb2[3])

        return listOf(a, r, g, b)
    }

    private fun mergeOneNum(num1: Int, num2: Int): Int {
        val right = num1 and 0xf0
        val left = (num2 and 0xf0) shr 4
        return right or left
    }

    private fun argbToColor(argb: List<Int>): Int =
        argb[0] and 0xff shl 24 or (argb[1] and 0xff shl 16) or (argb[2] and 0xff shl 8) or (argb[3] and 0xff)

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun encrypt(secretImg: Bitmap, decoyImg: Bitmap): Bitmap {
        if (secretImg.height > decoyImg.height || secretImg.width > decoyImg.width) {
            // Error: Please pick a different decoy image that is strictly bigger in size.
            Toast.makeText(
                context,
                "Please pick a different decoy image that is strictly bigger in size.",
                Toast.LENGTH_LONG
            ).show()
            return secretImg
        }

        val coverImg = Bitmap.createBitmap(null, decoyImg.width, decoyImg.height, decoyImg.config)
        val totalPixels = coverImg.width * coverImg.height
        var pixelsProcessed = 0
        for (w in 0 until coverImg.width) {
            for (h in 0 until coverImg.height) {
                pixelsProcessed++
                val color1 = decoyImg.getPixel(w, h)
                var color2 = android.graphics.Color.BLACK
                if (w < secretImg.width && h < secretImg.height) {
                    color2 = secretImg.getPixel(w, h)
                }

                val newColor = argbToColor(mergeArgb(colorToRgb(color1), colorToRgb(color2)))
                coverImg.setPixel(w, h, newColor)

                if (pixelsProcessed % 20 == 0 || pixelsProcessed == totalPixels) {
                    (context as Activity).runOnUiThread {
                        progressBar.progress = (pixelsProcessed * 100) / totalPixels
                    }
                }
            }
        }

        return coverImg
    }
}