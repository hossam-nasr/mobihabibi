package edu.harvard.mobihabibi

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import edu.harvard.mobihabibi.perm.PermissionsManager

class MainActivity : AppCompatActivity() {

    private lateinit var permManager: PermissionsManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permManager = PermissionsManager(this)
        permManager.verifyStoragePermissions()

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