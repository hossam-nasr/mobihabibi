package edu.harvard.mobihabibi

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import edu.harvard.mobihabibi.img.ImageEngine
import edu.harvard.mobihabibi.steg.StegEngine
import info.guardianproject.f5android.plugins.PluginNotificationListener
import info.guardianproject.f5android.plugins.f5.james.JpegEncoder
import info.guardianproject.f5android.stego.StegoProcessThread
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

// import info.guardianproject.f5android.plugins.f5.james.JpegEncoder


class EncryptActivity : AppCompatActivity(), PluginNotificationListener {
    // Storage Permissions
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val REQUEST_IMAGE_CAPTURE = 2
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var secretBitmap: Bitmap? = null
    private var decoyBitmap: Bitmap? = null
    private var decoyFile: File? = null
    private var resBitmap: Bitmap? = null
    private var progressTicks: Int = 0
    private val totalTicks: Int = 13
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
        verifyStoragePermissions(this)
        btnUploadSecret.setOnClickListener {
            requestSecret()
        }
        btnUploadDecoy.setOnClickListener {
            requestDecoy()
        }
        btnEncrypt.setOnClickListener {
            // requestEnc()
            requestEncNewTest()
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
        val path = ImageEngine.ImageFilePath.getPath(this, uri)
        if (path != null) {
            decoyFile = File(path)
        }
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
                    val imgFile = imgEngine.saveImage(resBitmap!!)
                    if (imgFile != null && decoyFile != null) {
                        imgEngine.copyExifData(decoyFile!!, imgFile, null)
                    }
                    runOnUiThread {
                        findViewById<ImageView>(R.id.ivEncRes).setImageBitmap(resBitmap)
                    }
                }
            }
        } else {
            Toast.makeText(this, "Please upload a secret and a decoy!", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestEncNewTest() {
        if (secretBitmap != null && decoyBitmap != null) {
            progressBar.visibility = View.VISIBLE
            thread {
                val outputFile = createOutputFile(
                    decoyBitmap!!.width,
                    decoyBitmap!!.height,
                    decoyBitmap!!.config
                )
                onProgressTick()
                if (outputFile != null) {
                    val fos = FileOutputStream(outputFile)
                    val jpg = JpegEncoder(
                        this,
                        decoyBitmap!!,
                        100,
                        fos,
                        "Seed".toByteArray(),
                        StegoProcessThread()
                    )
                    val secretByteStream = ByteArrayOutputStream()
                    secretBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, secretByteStream)
                    onProgressTick()
                    val success = jpg.Compress(ByteArrayInputStream(secretByteStream.toByteArray()))
                    if (success) {
                        if (decoyFile != null) {
                            imgEngine.copyExifData(decoyFile!!, outputFile, null)
                        }
                        runOnUiThread {
                            findViewById<ImageView>(R.id.ivEncRes).setImageURI(outputFile.toUri())
                        }
                        MediaScannerConnection.scanFile(
                            this, arrayOf(outputFile.toString()), null
                        ) { path, uri ->
                            Log.i("ExternalStorage", "Scanned $path:")
                            Log.i("ExternalStorage", "-> uri=$uri")
                        }
                        Log.d("DEBUG", "Success")
                        onProgressTick()
                    } else {
                        Log.d("DEBUG", "Failure")
                    }
                    fos.close()
                }

            }
        }

    }

    private fun createOutputFile(width: Int, height: Int, config: Bitmap.Config): File? {
        return imgEngine.saveImage(Bitmap.createBitmap(null, width, height, config))
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    fun verifyStoragePermissions(activity: Activity?) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    override fun onFailure() {
        Log.d("DEBUG", "FAILURE")
    }

    override fun onUpdate(with_message: String?) {
        Log.d("DEBUG", "UPDATE WITH MESSAGE $with_message")
        onProgressTick()
    }

    private fun onProgressTick() {
        progressTicks++
        runOnUiThread {
            progressBar.progress = (progressTicks * 100) / totalTicks
        }
    }

}