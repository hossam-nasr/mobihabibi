package edu.harvard.mobihabibi.steg

import android.app.Activity
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import edu.harvard.mobihabibi.R
import edu.harvard.mobihabibi.img.ImageEngine
import info.guardianproject.f5android.plugins.f5.Extract
import info.guardianproject.f5android.plugins.f5.james.JpegEncoder
import info.guardianproject.f5android.stego.StegoProcessThread
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class F5Manager(private val activity: Activity, private val imgEngine: ImageEngine) {
    companion object {
        private const val DECOY_QUALITY = 100 /* higher = can store larger secrets */
        private const val STEG_COMPRESSION_QUALITY = 50 /* lower = can store larger secrets */

        /* Future: have the user input a password (or use stronger password) */
        private const val SEED =
            "EGYPT AND TUNISIA RULE THE WORLD!!!!!!!!! HABIBIS TAKE OVER"
    }

    fun encrypt(
        decoyBitmap: Bitmap,
        decoyFile: File?,
        secretBitmap: Bitmap,
        onProgressTick: () -> Unit
    ) {
        val outputFile = imgEngine.createOutputFile(
            decoyBitmap.width,
            decoyBitmap.height,
            decoyBitmap.config
        )
        onProgressTick()
        if (outputFile != null) {
            val fos = FileOutputStream(outputFile)
            val jpg = JpegEncoder(
                activity,
                decoyBitmap,
                DECOY_QUALITY,
                fos,
                SEED.toByteArray(),
                StegoProcessThread()
            )
            val secretByteStream = ByteArrayOutputStream()
            secretBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                STEG_COMPRESSION_QUALITY,
                secretByteStream
            )
            onProgressTick()
            val success = jpg.Compress(ByteArrayInputStream(secretByteStream.toByteArray()))
            if (success) {
                if (decoyFile != null) {
                    imgEngine.copyExifData(decoyFile, outputFile, null)
                }
                activity.runOnUiThread {
                    activity.findViewById<ImageView>(R.id.ivEncRes).setImageURI(outputFile.toUri())
                }
                MediaScannerConnection.scanFile(
                    activity, arrayOf(outputFile.toString()), null, null
                )
                if (decoyFile != null) {
                    activity.runOnUiThread {
                        if (decoyFile.delete()) {
                            Toast.makeText(
                                activity,
                                "Decoy has been successfully deleted and steganographic image saved in Pictures directory",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                activity,
                                "Decoy image could NOT be deleted, please delete it manually. Your steganographic image is saved in the Pictures directory",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                onProgressTick()
            } else {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Error: couldn't encrypt image :(", Toast.LENGTH_LONG)
                        .show()
                }
            }
            fos.close()
        }

    }

    fun decrypt(stegoFile: File) {
        try {
            val extract = Extract(activity, stegoFile, SEED.toByteArray())
            extract.run()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}