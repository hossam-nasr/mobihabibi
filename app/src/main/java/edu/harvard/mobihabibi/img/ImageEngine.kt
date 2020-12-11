package edu.harvard.mobihabibi.img

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class ImageEngine(private val context: Context) {

    private val JSAMPLE_SIZE = 255 + 1
    private val CENTERJSAMPLE = 128
    private val SCALEBITS = 16
    private val CBCR_OFFSET = CENTERJSAMPLE shl SCALEBITS
    private val ONE_HALF = 1 shl SCALEBITS - 1
    private val R_Y_OFFSET = 0 * JSAMPLE_SIZE
    private val G_Y_OFFSET = 1 * JSAMPLE_SIZE
    private val B_Y_OFFSET = 2 * JSAMPLE_SIZE
    private val R_CB_OFFSET = 3 * JSAMPLE_SIZE
    private val G_CB_OFFSET = 4 * JSAMPLE_SIZE

    /** B=>Cb, R=>Cr are the same  */
    private val B_CB_OFFSET = 5 * JSAMPLE_SIZE

    /** B=>Cb, R=>Cr are the same  */
    private val R_CR_OFFSET = 6 * JSAMPLE_SIZE
    private val G_CR_OFFSET = 7 * JSAMPLE_SIZE
    private val B_CR_OFFSET = 8 * JSAMPLE_SIZE
    private val TABLE_SIZE = 9 * JSAMPLE_SIZE

    private val rgb_ycc_tab = IntArray(TABLE_SIZE)
    private fun FIX(x: Double): Int {
        return (x * (1L shl SCALEBITS) + 0.5).toInt()
    }

    fun rgb_ycc_convert(argb: IntArray?, width: Int, height: Int, ycc: ByteArray) {
        val tab: IntArray = this.rgb_ycc_tab
        for (i in 0 until JSAMPLE_SIZE) {
            tab[R_Y_OFFSET + i] = FIX(0.299) * i
            tab[G_Y_OFFSET + i] = FIX(0.587) * i
            tab[B_Y_OFFSET + i] = FIX(0.114) * i + ONE_HALF
            tab[R_CB_OFFSET + i] = -FIX(0.168735892) * i
            tab[G_CB_OFFSET + i] = -FIX(0.331264108) * i
            tab[B_CB_OFFSET + i] = FIX(0.5) * i + CBCR_OFFSET + ONE_HALF - 1
            tab[R_CR_OFFSET + i] = FIX(0.5) * i + CBCR_OFFSET + ONE_HALF - 1
            tab[G_CR_OFFSET + i] = -FIX(0.418687589) * i
            tab[B_CR_OFFSET + i] = -FIX(0.081312411) * i
        }
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var index = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = argb!![index] and 0x00ff0000 shr 16
                val g = argb[index] and 0x0000ff00 shr 8
                val b = argb[index] and 0x000000ff shr 0
                val Y =
                    (tab[r + R_Y_OFFSET] + tab[g + G_Y_OFFSET] + tab[b + B_Y_OFFSET] shr SCALEBITS).toByte()
                val Cb =
                    (tab[r + R_CB_OFFSET] + tab[g + G_CB_OFFSET] + tab[b + B_CB_OFFSET] shr SCALEBITS).toByte()
                val Cr =
                    (tab[r + R_CR_OFFSET] + tab[g + G_CR_OFFSET] + tab[b + B_CR_OFFSET] shr SCALEBITS).toByte()
                ycc[yIndex++] = Y
                if (y % 2 == 0 && index % 2 == 0) {
                    ycc[uvIndex++] = Cr
                    ycc[uvIndex++] = Cb
                }
                index++
            }
        }
    }

    fun compress(bitmap: Bitmap): ByteArray? {
        val w = bitmap.width
        val h = bitmap.height
        var argb: IntArray? = IntArray(w * h)
        bitmap.getPixels(argb, 0, w, 0, 0, w, h)
        val ycc = ByteArray(w * h * 3 / 2)
        rgb_ycc_convert(argb, w, h, ycc)
        argb = null // let GC do its job
        val jpeg = ByteArrayOutputStream()
        val yuvImage = YuvImage(ycc, ImageFormat.NV21, w, h, null)
        yuvImage.compressToJpeg(Rect(0, 0, w, h), 100, jpeg)
        return jpeg.toByteArray()
    }


    fun saveImage(img: Bitmap) {
        val root = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        ).toString()
        val myDir = File(root)
        myDir.mkdirs()

        val fname = "${System.currentTimeMillis()}.jpg"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            // img.compress(Bitmap.CompressFormat.PNG, 100, out)
            val result = compress(img)
            out.write(result)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(
            context, arrayOf(file.toString()), null
        ) { path, uri ->
            Log.i("ExternalStorage", "Scanned $path:")
            Log.i("ExternalStorage", "-> uri=$uri")
        }
    }
}