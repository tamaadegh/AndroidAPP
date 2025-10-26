package com.example.tamaade.presentation.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.tamaade.utils.FirebaseUtils

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Installs the splash screen and applies the theme
        installSplashScreen()

        checkUserAndNavigate()
    }

    private fun checkUserAndNavigate() {
        val user = FirebaseUtils.firebaseUser
        if (user != null) {
            // User is signed in, navigate to HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        } else {
            // No user is signed in, navigate to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        // Finish this activity so the user can't navigate back to it
        finish()
    }
}