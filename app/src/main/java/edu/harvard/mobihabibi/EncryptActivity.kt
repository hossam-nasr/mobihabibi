package edu.harvard.mobihabibi

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast

class EncryptActivity : AppCompatActivity() {
    private var secretBitmap: Bitmap? = null
    private var decoyBitmap: Bitmap? = null
    private var resBitmap: Bitmap? = null

    companion object {
        private const val SECRET_PICK_CODE = 999
        private const val DECOY_PICK_CODE = 998
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

    private fun requestEnc() {
        if (secretBitmap != null && decoyBitmap != null) {
            resBitmap = encrypt(secretBitmap!!, decoyBitmap!!)
            findViewById<ImageView>(R.id.ivEncRes).setImageBitmap(resBitmap)
        } else {
            Toast.makeText(this, "Please upload a secret and a decoy!", Toast.LENGTH_LONG).show()
        }
    }

    private fun encrypt(secretImg: Bitmap, decoyImg: Bitmap): Bitmap {
        return decoyImg
    }
}