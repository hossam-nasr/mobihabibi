package edu.harvard.mobihabibi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class EncryptActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encrypt)

        val btnUploadSecret = findViewById<Button>(R.id.btnUploadSecret)
        val btnUploadDecoy = findViewById<Button>(R.id.btnUploadSecret)
        val btnEncrypt = findViewById<Button>(R.id.btnEncRes)
        btnUploadSecret.setOnClickListener {
            uploadSecret()
        }
        btnUploadDecoy.setOnClickListener {
            uploadDecoy()
        }
        btnEncrypt.setOnClickListener {
            encrypt()
        }
    }

    private fun uploadSecret() {

    }

    private fun uploadDecoy() {

    }

    private fun encrypt() {

    }
}