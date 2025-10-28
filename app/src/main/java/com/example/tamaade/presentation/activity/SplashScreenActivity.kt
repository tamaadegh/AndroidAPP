package com.example.tamaade.presentation.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.tamaade.R
import com.example.tamaade.utils.FirebaseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private var isReady = false
    private var isUserLoggedIn: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()

        // Keep the splash screen on-screen until we're ready.
        splashScreen.setKeepOnScreenCondition { !isReady }

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val slideUp = AnimationUtils.loadAnimation(this, R.anim.text_slide_up)
            splashScreenView.view.startAnimation(slideUp)

            slideUp.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    splashScreenView.remove()
                    navigate()
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }

        // Check user in background
        lifecycleScope.launch {
            isUserLoggedIn = withContext(Dispatchers.IO) {
                FirebaseUtils.firebaseUser != null
            }
            // Now we are ready to dismiss the splash screen
            isReady = true
        }
    }

    private fun navigate() {
        val intent = if (isUserLoggedIn == true) {
            Intent(this, HomeActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}