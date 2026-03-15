package com.example.tamaade.presentation.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.tamaade.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_splash_screen)

        val logoImageView: ImageView = findViewById(R.id.logoImageView)
        val mottoTextView: TextView = findViewById(R.id.mottoTextView)

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up)

        logoImageView.startAnimation(scaleUp)
        mottoTextView.startAnimation(fadeIn)

        lifecycleScope.launch {
            delay(2000) // Adjusted delay for animations
            navigate()
        }
    }

    private fun navigate() {
        val user = FirebaseAuth.getInstance().currentUser
        val intent = if (user != null) {
            Intent(this, HomeActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
