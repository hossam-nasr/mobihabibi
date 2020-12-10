package edu.harvard.mobihabibi

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import edu.harvard.mobihabibi.img.ImageEngine
import edu.harvard.mobihabibi.steg.StegEngine
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread


class EncryptActivity : AppCompatActivity() {
    private var secretBitmap: Bitmap? = null
    private var decoyBitmap: Bitmap? = null
    private var resBitmap: Bitmap? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var stegEngine: StegEngine
    private lateinit var imgEngine: ImageEngine

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
        progressBar = findViewById(R.id.pbEnc)
        stegEngine = StegEngine(this, progressBar)
        imgEngine = ImageEngine(this)
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
            progressBar.visibility = View.VISIBLE
            thread {
                resBitmap = stegEngine.encrypt(secretBitmap!!, decoyBitmap!!)
                if (resBitmap != null) {
                    imgEngine.saveImage(resBitmap!!)
                    runOnUiThread {
                        findViewById<ImageView>(R.id.ivEncRes).setImageBitmap(resBitmap)
                    }
                }
            }
        } else {
            Toast.makeText(this, "Please upload a secret and a decoy!", Toast.LENGTH_LONG).show()
        }
    }

}