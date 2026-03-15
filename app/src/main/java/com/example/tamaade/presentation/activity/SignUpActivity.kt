package com.example.tamaade.presentation.activity


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.text.Editable
import android.text.TextWatcher

import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import androidx.core.content.ContextCompat
import com.example.tamaade.R
import com.example.tamaade.data.model.User

import com.example.tamaade.utils.Extensions.toast
import com.example.tamaade.presentation.LoadingDialog

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext



class SignUpActivity : AppCompatActivity() {


    private lateinit var fullName: EditText
    private lateinit var emailEt: EditText
    private lateinit var passEt: EditText
    private lateinit var CpassEt: EditText

    private val userCollectionRef = Firebase.firestore.collection("Users")
    val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    lateinit var loadingDialog: LoadingDialog

    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val signUpBtn = findViewById<Button>(R.id.signUpBtn_signUpPage)
        fullName = findViewById(R.id.nameEt_signUpPage)
        emailEt = findViewById(R.id.emailEt_signUpPage)
        passEt = findViewById(R.id.PassEt_signUpPage)
        CpassEt = findViewById(R.id.cPassEt_signUpPage)
        val signInTv = findViewById<TextView>(R.id.signInTv_signUpPage)



        loadingDialog = LoadingDialog(this)

        textAutoCheck()

        signInTv.setOnClickListener {
            intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }


        signUpBtn.setOnClickListener {
            checkInput()

        }
    }

    private fun textAutoCheck() {

        fullName.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                if (fullName.text.isEmpty()){
                    fullName.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)

                }
                else if (fullName.text.length >= 4){
                    fullName.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(applicationContext,
                        R.drawable.ic_check
                    ), null)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {

                fullName.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                if (count >= 4){
                    fullName.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(applicationContext,
                        R.drawable.ic_check
                    ), null)
                }
            }
        })

        emailEt.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                if (emailEt.text.isEmpty()){
                    emailEt.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)

                }
                else if (emailEt.text.matches(emailPattern.toRegex())) {
                    emailEt.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(applicationContext,
                        R.drawable.ic_check
                    ), null)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {

                emailEt.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                if (emailEt.text.matches(emailPattern.toRegex())) {
                    emailEt.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(applicationContext,
                        R.drawable.ic_check
                    ), null)
                }
            }
        })

        passEt.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                if (passEt.text.isEmpty()){
                    passEt.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)

                }
                else if (passEt.text.length > 5){
                    passEt.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(applicationContext,
                        R.drawable.ic_check
                    ), null)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {

                passEt.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                if (count > 5){
                    passEt.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(applicationContext,
                        R.drawable.ic_check
                    ), null)
                }
            }
        })

        CpassEt.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                if (CpassEt.text.isEmpty()){
                    CpassEt.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)

                }
                else if (CpassEt.text.toString() == passEt.text.toString()){
                    CpassEt.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(applicationContext,
                        R.drawable.ic_check
                    ), null)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {

                CpassEt.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                if (CpassEt.text.toString() == passEt.text.toString()){
                    CpassEt.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(applicationContext,
                        R.drawable.ic_check
                    ), null)
                }
            }
        })

    }

    private fun checkInput() {
        if (fullName.text.isEmpty()){
            toast("Name can't empty!")
            return
        }
        if (emailEt.text.isEmpty()){
            toast("Email can't empty!")
            return
        }

        if (!emailEt.text.matches(emailPattern.toRegex())) {
            toast("Enter Valid Email")
            return
        }
        if(passEt.text.isEmpty()){
            toast("Password can't empty!")
            return
        }
        if (passEt.text.toString() != CpassEt.text.toString()){
            toast("Password not Match")
            return
        }

        signUp()


    }



    private fun signUp() {
        loadingDialog.startLoadingDialog()

        val emailV: String = emailEt.text.toString()
        val passV: String = passEt.text.toString()
        val fullname : String = fullName.text.toString()
            /*create a user*/
        firebaseAuth.createUserWithEmailAndPassword(emailV,passV)

                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = firebaseAuth.currentUser
                        
                        // Update display name in Firebase Auth
                        val profileUpdates = userProfileChangeRequest {
                            displayName = fullname
                        }
                        
                        firebaseUser?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                            val user = User(fullname,"",firebaseAuth.uid.toString(),emailV,"","")
                            storeUserData(user)
                        } ?: run {
                            // Fallback if user is null for some reason
                            val user = User(fullname,"",firebaseAuth.uid.toString(),emailV,"","")
                            storeUserData(user)
                        }

                    } else {
                        loadingDialog.dismissDialog()
                        toast("failed to Authenticate: ${task.exception?.message}")
                    }
                }

    }

    private fun storeUserData(user: User) = CoroutineScope(Dispatchers.IO).launch {
        try {

            userCollectionRef.document(firebaseAuth.uid.toString()).set(user).await()
            withContext(Dispatchers.Main){
                toast("Data Saved")
                loadingDialog.dismissDialog()
                val intent = Intent(this@SignUpActivity, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }

        }catch (e:Exception){
            withContext(Dispatchers.Main){
                toast(""+ e.message.toString())
                loadingDialog.dismissDialog()
            }
        }
    }

}
