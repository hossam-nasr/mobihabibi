package edu.harvard.mobihabibi

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import edu.harvard.mobihabibi.img.ImageEngine
import edu.harvard.mobihabibi.perm.PermissionsManager
import edu.harvard.mobihabibi.steg.F5Manager
import edu.harvard.mobihabibi.steg.StegEngine
import info.guardianproject.f5android.plugins.PluginNotificationListener
import java.io.*
import kotlin.concurrent.thread


class EncryptActivity : AppCompatActivity(), PluginNotificationListener {
    companion object {
        private const val SECRET_PICK_CODE = 999
        private const val DECOY_PICK_CODE = 998
        private const val REQUEST_IMAGE_CAPTURE = 2
    }

    private lateinit var stegEngine: StegEngine
    private lateinit var imgEngine: ImageEngine
    private lateinit var permManager: PermissionsManager
    private lateinit var f5Manager: F5Manager

    private lateinit var progressBar: ProgressBar
    private var secretBitmap: Bitmap? = null
    private var decoyBitmap: Bitmap? = null
    private var decoyFile: File? = null
    private var decoyPath: String? = null
    private var resBitmap: Bitmap? = null

    private var progressTicks: Int = 0
    private val totalTicks: Int = 13

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encrypt)

        progressBar = findViewById(R.id.pbEnc)
        stegEngine = StegEngine(this, progressBar)
        imgEngine = ImageEngine(this)
        f5Manager = F5Manager(this, imgEngine)
        permManager = PermissionsManager(this)
        permManager.verifyStoragePermissions()


        val btnUploadSecret = findViewById<Button>(R.id.btnUploadSecret)
        val btnUploadDecoy = findViewById<Button>(R.id.btnUploadDecoy)
        val btnCaptureDecoy = findViewById<Button>(R.id.btnCaptureDecoy)
        val btnEncrypt = findViewById<Button>(R.id.btnEncRes)
        btnUploadSecret.setOnClickListener {
            requestSecret()
        }
        btnUploadDecoy.setOnClickListener {
            requestUploadDecoy()
        }
        btnCaptureDecoy.setOnClickListener {
            requestCaptureDecoy()
        }
        btnEncrypt.setOnClickListener {
            requestEnc()
        }
    }

    private fun requestSecret() {
        requestImage(SECRET_PICK_CODE)
    }

    private fun requestUploadDecoy() {
        requestImage(DECOY_PICK_CODE)
    }

    private fun requestImage(code: Int) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, code)
    }

    private fun requestCaptureDecoy() {
        try {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                takePictureIntent.resolveActivity(packageManager)?.also {
                    decoyFile = try {
                        imgEngine.createOutputDecoyFile {
                            decoyPath = it.absolutePath
                        }
                    } catch (ex: IOException) {
                        runOnUiThread {
                            Toast.makeText(this, "Error creating decoy image :(", Toast.LENGTH_LONG)
                                .show()
                        }
                        null
                    }
                    // Continue only if the File was successfully created
                    decoyFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            "com.example.android.fileprovider",
                            it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
            }
        } catch (e: ActivityNotFoundException) {
            runOnUiThread {
                Toast.makeText(this, "Error: can't access the camera :(", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            decoyBitmap = data?.extras?.get("data") as Bitmap?
            if (decoyBitmap != null) {
                findViewById<ImageView>(R.id.ivDecoy).setImageBitmap(decoyBitmap)
            } else {
                if (decoyFile != null) {
                    decoyBitmap = BitmapFactory.decodeFile(decoyFile.toString())
                }
                findViewById<ImageView>(R.id.ivDecoy).setImageURI(decoyFile?.toUri())
            }
        } else if (resultCode == Activity.RESULT_OK) {
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

    private fun processSecret(uri: Uri) {
        findViewById<ImageView>(R.id.ivSecret).setImageURI(uri)
        secretBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
    }

    private fun processDecoy(uri: Uri) {
        findViewById<ImageView>(R.id.ivDecoy).setImageURI(uri)
        decoyBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        val path = ImageEngine.ImageFilePath.getPath(this, uri)
        if (path != null) {
            decoyFile = File(path)
        }
    }

    private fun requestEnc() {
        if (secretBitmap != null && decoyBitmap != null) {
            progressBar.visibility = View.VISIBLE
            thread {
                if (decoyBitmap != null && secretBitmap != null) {
                    f5Manager.encrypt(decoyBitmap!!, decoyFile, secretBitmap!!) {
                        onProgressTick()
                    }
                }
            }
        }

    }

    override fun onFailure() {
        runOnUiThread {
            Toast.makeText(this, "Error: Could not encrypt image :(", Toast.LENGTH_LONG).show()
            progressBar.progress = 0
        }
    }

    override fun onUpdate(with_message: String?) {
        onProgressTick()
    }

    private fun onProgressTick() {
        progressTicks++
        runOnUiThread {
            progressBar.progress = (progressTicks * 100) / totalTicks
        }
    }

}