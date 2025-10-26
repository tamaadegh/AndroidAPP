package com.example.tamaade.presentation

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.example.tamaade.R

class LoadingDialog(private val activity: Activity) {

    private var dialog: AlertDialog? = null

    fun startLoadingDialog() {
        val builder = AlertDialog.Builder(activity)
        val inflater = LayoutInflater.from(activity)
        val dialogView = inflater.inflate(R.layout.custom_dialog, null)

        // Get the progress bar and apply green theme
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBarLoding)
        progressBar?.indeterminateTintList = ContextCompat.getColorStateList(activity, R.color.primary)

        builder.setView(dialogView)
        builder.setCancelable(false)

        dialog = builder.create()

        // Make dialog background transparent
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog?.show()
    }

    fun dismissDialog() {
        dialog?.dismiss()
    }

    fun isShowing(): Boolean {
        return dialog?.isShowing == true
    }
}