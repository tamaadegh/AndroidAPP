package com.example.tamaade.presentation.activity

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.example.tamaade.R
import com.example.tamaade.utils.Extensions.toast
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var saveSettingsButton: Button
    private lateinit var changePasswordButton: TextView
    private lateinit var darkModeSwitch: SwitchMaterial
    private lateinit var promoNotificationSwitch: SwitchMaterial
    private lateinit var orderNotificationSwitch: SwitchMaterial
    private lateinit var trotroAlertSwitch: SwitchMaterial

    private val userCollectionRef = Firebase.firestore.collection("Users")
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)

        initViews()
        setupListeners()
        loadUserSettings()
        getUserData()
    }

    private fun initViews() {
        nameEditText = findViewById(R.id.nameEt_SettingsPage)
        emailEditText = findViewById(R.id.EmailEt_SettingsPage)
        saveSettingsButton = findViewById(R.id.saveSetting_SettingsBtn)
        changePasswordButton = findViewById(R.id.changePasswordBtn)
        darkModeSwitch = findViewById(R.id.darkModeSwitch)
        promoNotificationSwitch = findViewById(R.id.promoNotificationSwitch)
        orderNotificationSwitch = findViewById(R.id.orderNotificationSwitch)
        trotroAlertSwitch = findViewById(R.id.trotroAlertSwitch)
        findViewById<ImageView>(R.id.backIv_ProfileFrag).setOnClickListener { finish() }
    }

    private fun setupListeners() {
        saveSettingsButton.setOnClickListener { saveUserData() }
        changePasswordButton.setOnClickListener { showChangePasswordDialog() }

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                saveSettingsButton.visibility = View.VISIBLE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if((s?.length ?: 0) > 1) saveSettingsButton.visibility = View.VISIBLE
            }
        }

        nameEditText.addTextChangedListener(textWatcher)
        emailEditText.addTextChangedListener(textWatcher)

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("dark_mode", isChecked)
            updateTheme(isChecked)
        }

        promoNotificationSwitch.setOnCheckedChangeListener { _, isChecked -> saveSetting("promo_notifications", isChecked) }
        orderNotificationSwitch.setOnCheckedChangeListener { _, isChecked -> saveSetting("order_notifications", isChecked) }
        trotroAlertSwitch.setOnCheckedChangeListener { _, isChecked -> saveSetting("trotro_alerts", isChecked) }
    }

    private fun loadUserSettings() {
        darkModeSwitch.isChecked = sharedPreferences.getBoolean("dark_mode", false)
        promoNotificationSwitch.isChecked = sharedPreferences.getBoolean("promo_notifications", true)
        orderNotificationSwitch.isChecked = sharedPreferences.getBoolean("order_notifications", true)
        trotroAlertSwitch.isChecked = sharedPreferences.getBoolean("trotro_alerts", true)
        updateTheme(darkModeSwitch.isChecked)
    }

    private fun getUserData() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val querySnapshot = userCollectionRef.document(firebaseAuth.uid.toString()).get().await()
            val userName = querySnapshot.data?.get("userName").toString()
            val userEmail = querySnapshot.data?.get("userEmail").toString()
            withContext(Dispatchers.Main) {
                nameEditText.setText(userName)
                emailEditText.setText(userEmail)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun saveUserData() {
        if (nameEditText.text.isEmpty() || emailEditText.text.isEmpty()) {
            toast("Fields can't be empty")
            return
        }
        saveNameAndEmailToFireStore()
    }

    private fun saveNameAndEmailToFireStore() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val updates = mapOf(
                "userName" to nameEditText.text.toString(),
                "userEmail" to emailEditText.text.toString()
            )
            userCollectionRef.document(firebaseAuth.uid.toString()).update(updates).await()
            withContext(Dispatchers.Main) {
                toast("Saved")
                saveSettingsButton.visibility = View.GONE
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                toast(e.message.toString())
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val oldPasswordEt = dialogView.findViewById<EditText>(R.id.oldPasswordEt)
        val newPasswordEt = dialogView.findViewById<EditText>(R.id.newPasswordEt)
        val confirmPasswordEt = dialogView.findViewById<EditText>(R.id.confirmPasswordEt)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val oldPassword = oldPasswordEt.text.toString()
                val newPassword = newPasswordEt.text.toString()
                val confirmPassword = confirmPasswordEt.text.toString()

                if (newPassword != confirmPassword) {
                    toast("Passwords do not match")
                    return@setPositiveButton
                }
                if (newPassword.length < 6) {
                    toast("Password should be at least 6 characters")
                    return@setPositiveButton
                }

                reauthenticateAndChangePassword(oldPassword, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reauthenticateAndChangePassword(oldPassword: String, newPassword: String) {
        val user = firebaseAuth.currentUser
        val credential = EmailAuthProvider.getCredential(user?.email!!, oldPassword)

        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        toast("Password updated successfully")
                    } else {
                        toast("Error: ${updateTask.exception?.message}")
                    }
                }
            } else {
                if (task.exception is FirebaseAuthInvalidCredentialsException) {
                    toast("Invalid old password")
                } else {
                    toast("Authentication failed: ${task.exception?.message}")
                }
            }
        }
    }

    private fun saveSetting(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    private fun updateTheme(isDarkMode: Boolean) {
        val mode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}