package edu.harvard.mobihabibi

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import edu.harvard.mobihabibi.img.ImageEngine
import edu.harvard.mobihabibi.steg.StegEngine
import info.guardianproject.f5android.plugins.PluginNotificationListener
import info.guardianproject.f5android.plugins.f5.james.JpegEncoder
import info.guardianproject.f5android.stego.StegoProcessThread
import java.io.*
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
    private var decoyPath: String? = null
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
        val btnCaptureDecoy = findViewById<Button>(R.id.btnCaptureDecoy)
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
        btnCaptureDecoy.setOnClickListener {
            requestDecoyNewTest()
        }
        btnEncrypt.setOnClickListener {
            // requestEnc()
            requestEncNewTest()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            decoyBitmap = data?.extras?.get("data") as Bitmap?
            if (decoyBitmap != null) {
                findViewById<ImageView>(R.id.ivDecoy).setImageBitmap(decoyBitmap)
            } else {
                Log.d("DEBUG", "Here decoyFile is $decoyFile")
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

    private fun requestDecoyNewTest() {
        Log.d("DEBUG", "Entering")
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                Log.d("DEBUG", "$takePictureIntent")
                // Ensure that there's a camera activity to handle the intent
                takePictureIntent.resolveActivity(packageManager)?.also {
                    Log.d("DEBUG", "Here it is $it")
                    // Create the File where the photo should go
                    decoyFile = try {
                        createOutputDecoyFile()
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        Log.d("DEBUG", "Error creating file for decoy for camera")
                        null
                    }
                    // Continue only if the File was successfully created
                    decoyFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            "com.example.android.fileprovider",
                            it
                        )
//                        val photoUri = decoyFile!!.toUri()
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        Log.d("DEBUG", "Here")
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
            }
//            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
            Log.d("DEBUG", "Wtf bro why u have no camera")
        }
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
                    secretBitmap!!.compress(Bitmap.CompressFormat.JPEG, 60, secretByteStream)
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
                        if (decoyFile != null) {
                            runOnUiThread {
                                if (decoyFile!!.delete()) {
                                    Toast.makeText(
                                        this,
                                        "Decoy has been successfully deleted and steganographic image saved in Pictures directory",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Decoy image could NOT be deleted, please delete it manually. Your steganographic image is saved in the Pictures directory",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
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

    private fun createOutputDecoyFile(): File? {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "${System.currentTimeMillis()}", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            Log.d("DEBUG", "File created with path $absolutePath")
            decoyPath = absolutePath
        }
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
        runOnUiThread {
            Toast.makeText(this, "Error: Could not encrypt image :(", Toast.LENGTH_LONG).show()
            progressBar.progress = 0
        }
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