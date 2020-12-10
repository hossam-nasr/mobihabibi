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
        // progressBar.progress = 0
        // progressBar.visibility = View.GONE
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
            progressBar.visibility = View.VISIBLE
            thread {
                resBitmap = stegEngine.encrypt(secretBitmap!!, decoyBitmap!!)
                saveImage()
                runOnUiThread {
                    findViewById<ImageView>(R.id.ivEncRes).setImageBitmap(resBitmap)
                }
            }
        } else {
            Toast.makeText(this, "Please upload a secret and a decoy!", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveImage() {
        val root = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        ).toString()
        val myDir = File(root)
        myDir.mkdirs()

        val fname = "${System.currentTimeMillis()}.png"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            resBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(
            this, arrayOf(file.toString()), null
        ) { path, uri ->
            Log.i("ExternalStorage", "Scanned $path:")
            Log.i("ExternalStorage", "-> uri=$uri")
        }
    }
}