package edu.harvard.mobihabibi

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity


class DecryptActivity : AppCompatActivity() {
    private var stegoImg: Bitmap? = null
    private var recoveredImg: Bitmap? = null

    companion object {
        private const val STEGO_PICK_CODE = 997
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decrypt)

        val btnUploadSteg = findViewById<Button>(R.id.btnUploadSteg)
        val btnDecRes = findViewById<Button>(R.id.btnDecRes)
        btnUploadSteg.setOnClickListener {
            requestStegImg()
        }
        btnDecRes.setOnClickListener {
            requestDec()
        }
    }

    private fun requestStegImg() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, STEGO_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == STEGO_PICK_CODE) {
            val uri = data?.data
            if (uri != null) {
                processStegImg(uri)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun processStegImg(uri: Uri) {
        findViewById<ImageView>(R.id.ivStegImg).setImageURI(uri)
        stegoImg = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
    }

    private fun requestDec() {
        if (stegoImg != null) {
            recoveredImg = decrypt(stegoImg!!)
            findViewById<ImageView>(R.id.ivDecRes).setImageBitmap(recoveredImg)
        } else {
            Toast.makeText(this, "Please upload an image to recover from", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun decrypt(img: Bitmap): Bitmap {
        var decryptedImg = Bitmap.createBitmap(null, img.width, img.height, img.config)

        var crop_h = img.height
        var crop_w = img.width

        for (w in 0 until img.width) {
            for (h in 0 until img.height) {
                val color = img.getPixel(w, h)

                val A = color shr 24 and 0xff
                val R = color shr 16 and 0xff
                val G = color shr 8 and 0xff
                val B = color and 0xff

                val newA = (A and 0x0f) shl 4
                val newR = (R and 0x0f) shl 4
                val newG = (G and 0x0f) shl 4
                val newB = (B and 0x0f) shl 4

                val newColor =
                    newA and 0xff shl 24 or (newR and 0xff shl 16) or (newG and 0xff shl 8) or (newB and 0xff)

                decryptedImg.setPixel(w, h, newColor)

                if (!(newR == 0 && newG == 0 && newB == 0)) {
                    crop_h = h + 1
                    crop_w = w + 1
                }

            }
        }

        decryptedImg = Bitmap.createBitmap(decryptedImg, 0,0,crop_w, crop_h);

        return decryptedImg
    }
}