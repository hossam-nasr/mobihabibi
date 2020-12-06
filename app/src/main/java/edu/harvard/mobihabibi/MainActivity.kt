package edu.harvard.mobihabibi

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnEnc = findViewById<Button>(R.id.btnEnc)
        val btnDec = findViewById<Button>(R.id.btnDec)
        btnEnc.setOnClickListener {
            navigateToEnc()
        }
        btnDec.setOnClickListener {
            navigateToDec()
        }
    }

    private fun navigateToEnc() {
        startActivity(Intent(this, EncryptActivity::class.java))
    }

    private fun navigateToDec() {
        startActivity(Intent(this, DecryptActivity::class.java))
    }
}