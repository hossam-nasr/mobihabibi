package edu.harvard.mobihabibi

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import edu.harvard.mobihabibi.img.ImageEngine
import edu.harvard.mobihabibi.perm.PermissionsManager
import edu.harvard.mobihabibi.steg.StegEngine
import info.guardianproject.f5android.plugins.PluginNotificationListener
import info.guardianproject.f5android.plugins.f5.Extract
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.security.spec.ECField
import kotlin.concurrent.thread


class DecryptActivity : AppCompatActivity(), Extract.ExtractionListener,
    PluginNotificationListener {
    private var stegoImg: Bitmap? = null
    private var stegoFile: File? = null
    private var recoveredImg: Bitmap? = null
    private var progressTicks: Int = 0
    private val totalTicks: Int = 9
    private lateinit var progressBar: ProgressBar
    private lateinit var stegEngine: StegEngine
    private lateinit var imgEngine: ImageEngine
    private lateinit var permManager: PermissionsManager

    companion object {
        private const val STEGO_PICK_CODE = 997
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decrypt)

        val btnUploadSteg = findViewById<Button>(R.id.btnUploadSteg)
        val btnDecRes = findViewById<Button>(R.id.btnDecRes)
        progressBar = findViewById(R.id.pbDec)
        stegEngine = StegEngine(this, progressBar)
        imgEngine = ImageEngine(this)
        permManager = PermissionsManager(this)
        permManager.verifyStoragePermissions()
        btnUploadSteg.setOnClickListener {
            requestStegImg()
        }
        btnDecRes.setOnClickListener {
            // requestDec()
            requestDecNewTest()
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
        val path = ImageEngine.ImageFilePath.getPath(this, uri)
        if (path != null) {
            stegoFile = File(path)
        }
    }

    private fun requestDec() {
        if (stegoImg != null) {
            thread {
                recoveredImg = stegEngine.decrypt(stegoImg!!)
                runOnUiThread {
                    findViewById<ImageView>(R.id.ivDecRes).setImageBitmap(recoveredImg)
                }
            }

        } else {
            Toast.makeText(this, "Please upload an image to recover from", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestDecNewTest() {
        if (stegoFile != null) {
            thread {
                try {
                    val extract = Extract(this, stegoFile!!, "Seed".toByteArray())
                    extract.run()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        } else {
            Toast.makeText(this, "Please upload an image to recover from", Toast.LENGTH_LONG).show()
        }
    }

    override fun onExtractionResult(baos: ByteArrayOutputStream?) {
        Log.d("DEBUG", "EXTRACTION RESULT SUCCESS")
        val ba = baos?.toByteArray()
        if (ba != null) {
            recoveredImg = BitmapFactory.decodeByteArray(ba, 0, ba.size)
            if (recoveredImg != null) {
                runOnUiThread {
                    findViewById<ImageView>(R.id.ivDecRes).setImageBitmap(recoveredImg)
                    Toast.makeText(
                        this,
                        "Image recovered successfully! Recovered image stored in the Pictures directory",
                        Toast.LENGTH_LONG
                    ).show()
                }
                imgEngine.saveImage(recoveredImg!!)
                onProgressTick()
            }
        }
    }

    override fun onFailure() {
        Log.d("DEBUG", "FAILURE")
        runOnUiThread {
            Toast.makeText(this, "Error: could not extract hidden image :(", Toast.LENGTH_LONG)
                .show()
            progressBar.progress = 0
        }
    }

    override fun onUpdate(with_message: String?) {
        Log.d("DEBUG", "Update with message $with_message")
        onProgressTick()
    }

    private fun onProgressTick() {
        progressTicks++
        runOnUiThread {
            progressBar.progress = (progressTicks * 100) / totalTicks
        }
    }
}