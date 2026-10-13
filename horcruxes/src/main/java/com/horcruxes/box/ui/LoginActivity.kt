package com.horcruxes.box.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.horcruxes.box.BuildConfig
import com.horcruxes.box.R
import com.horcruxes.box.data.HorcruxRepository

/**
 * The gate. XML layout — every control is guaranteed visible regardless of
 * OEM theme (programmatic styling on ColorOS proved unreliable).
 */
class LoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<TextView>(R.id.loginBuild).text =
            "v${BuildConfig.VERSION_NAME} • build ${BuildConfig.VERSION_CODE}"

        if (HorcruxRepository.isSessionValid(this)) {
            enterVault()
            return
        }

        val keyInput = findViewById<EditText>(R.id.keyInput)
        findViewById<Button>(R.id.enterButton).setOnClickListener {
            val key = keyInput.text?.toString()?.trim().orEmpty()
            if (key.length < 4) {
                Toast.makeText(this, "The Dark Lord rejects empty souls.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // TODO(Phase 3): POST { key } to the Horcrux server → AuthSession.
            HorcruxRepository.saveSession(this, key, 0L, "founder")
            Toast.makeText(this, "The vault opens…", Toast.LENGTH_SHORT).show()
            enterVault()
        }
    }

    private fun enterVault() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
