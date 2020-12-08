package edu.harvard.mobihabibi

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity


class EncryptActivity : AppCompatActivity() {
    private var secretBitmap: Bitmap? = null
    private var decoyBitmap: Bitmap? = null
    private var resBitmap: Bitmap? = null

    companion object {
        private const val SECRET_PICK_CODE = 999
        private const val DECOY_PICK_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encrypt)

        val btnUploadSecret = findViewById<Button>(R.id.btnUploadSecret)
        val btnUploadDecoy = findViewById<Button>(R.id.btnUploadDecoy)
        val btnEncrypt = findViewById<Button>(R.id.btnEncRes)
        btnUploadSecret.setOnClickListener {
            requestSecret()
        }
        btnUploadDecoy.setOnClickListener {
            requestDecoy()
        }
        btnEncrypt.setOnClickListener {
            requestEnc()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                when (requestCode) {
                    SECRET_PICK_CODE -> {
                        processSecret(uri)
                    }
                    DECOY_PICK_CODE -> {
                        processDecoy(uri)
                    }
                }
            }

        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun requestSecret() {
        requestImage(SECRET_PICK_CODE)
    }

    private fun processSecret(uri: Uri) {
        findViewById<ImageView>(R.id.ivSecret).setImageURI(uri)
        secretBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
    }

    private fun requestDecoy() {
        requestImage(DECOY_PICK_CODE)
    }

    private fun processDecoy(uri: Uri) {
        findViewById<ImageView>(R.id.ivDecoy).setImageURI(uri)
        decoyBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
    }

    private fun requestImage(code: Int) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, code)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun requestEnc() {
        if (secretBitmap != null && decoyBitmap != null) {
            resBitmap = encrypt(secretBitmap!!, decoyBitmap!!)
            findViewById<ImageView>(R.id.ivEncRes).setImageBitmap(resBitmap)
        } else {
            Toast.makeText(this, "Please upload a secret and a decoy!", Toast.LENGTH_LONG).show()
        }
    }

    private fun __Color_to_ARGB(color: Int): List<Int> {
        val A: Int = (color shr 24) and 0xff
        val R: Int = (color shr 16) and 0xff
        val G: Int = (color shr 8) and 0xff
        val B: Int = (color and 0xff)

        return listOf(A, R, G, B)
    }

    private fun __merge_ARGB(argb1: List<Int>, argb2: List<Int>): List<Int> {
        val A: Int = mergeOneNum(argb1[0], argb2[0])
        val R: Int = mergeOneNum(argb1[1], argb2[1])
        val G: Int = mergeOneNum(argb1[2], argb2[2])
        val B: Int = mergeOneNum(argb1[3], argb2[3])

        return listOf(A, R, G, B)
    }

    private fun mergeOneNum(num1 : Int, num2: Int): Int {
        val right = num1 and 0xf0
        val left = num2 and 0x0f
        return right or left
    }

    private fun __ARGB_to_Color(argb: List<Int>): Int {
        val color: Int =
            argb[0] and 0xff shl 24 or (argb[1] and 0xff shl 16) or (argb[2] and 0xff shl 8) or (argb[3] and 0xff)

        return color
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun encrypt(secretImg: Bitmap, decoyImg: Bitmap): Bitmap {
        if (secretImg.height > decoyImg.height || secretImg.width > decoyImg.width) {
            // Error: Please pick a different decoy image that is strictly bigger in size.
            return secretImg
        }

        val coverImg = Bitmap.createBitmap(null, decoyImg.width, decoyImg.height, decoyImg.config)
        for (w in 0 until coverImg.width) {
            for (h in 0 until coverImg.height) {
                val color1 = decoyImg.getPixel(w, h)

                var color2 = android.graphics.Color.BLACK
                if (w < secretImg.width && h < secretImg.height) {
                    Log.d("DEBUG", "Am I here?")
                    color2 = secretImg.getPixel(w, h)
                }

                val newColor =
                    __ARGB_to_Color(__merge_ARGB(__Color_to_ARGB(color1), __Color_to_ARGB(color2)))
                coverImg.setPixel(w, h, newColor)
            }
        }

        Log.d("DEBUG", "DONE!")
        return coverImg
    }
}