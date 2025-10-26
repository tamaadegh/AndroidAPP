package com.example.tamaade.presentation.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.tamaade.R
import com.example.tamaade.databinding.ActivityHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        // --- FIX: Correctly find the NavController ---
        // 1. Find the NavHostFragment from the FragmentManager using its container ID.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_home) as NavHostFragment
        
        // 2. Get the NavController from the NavHostFragment.
        navController = navHostFragment.navController

        // 3. Connect the BottomNavigationView to the NavController.
        // This automatically handles navigation when a menu item is tapped.
        navView.setupWithNavController(navController)
    }
}