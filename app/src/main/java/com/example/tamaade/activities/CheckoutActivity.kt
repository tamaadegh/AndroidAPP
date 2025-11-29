package com.example.tamaade.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tamaade.databinding.ActivityCheckoutBinding
import com.hubtel.merchant.checkout.sdk.CheckoutIntent
import com.hubtel.merchant.checkout.sdk.ux.model.CheckoutStatus
import java.util.*

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private val CHECKOUT_REQUEST_CODE = 101

    companion object {
        const val EXTRA_AMOUNT = "extra_amount"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val amountToPay = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)

        if (amountToPay <= 0) {
            Toast.makeText(this, "Invalid order amount.", Toast.LENGTH_LONG).show()
            finish() // Close activity if amount is invalid
            return
        }

        binding.totalAmountTextView.text = "GHS ${String.format("%.2f", amountToPay)}"

        binding.payNowButton.setOnClickListener {
            initiateCheckout(amountToPay)
        }
    }

    private fun initiateCheckout(amount: Double) {
        val clientReference = UUID.randomUUID().toString().take(36)

        // TODO: Replace with your actual customer phone number
        val customerPhoneNumber = "233540000000" 

        // This is a placeholder callback URL. Since you don't have a backend, Hubtel will redirect to this URL after payment.
        // You can change this to a URL that you control to handle the payment confirmation.
        val callbackUrl = "https://tamaade.com/payment-callback"

        // The API key is a Base64 encoded string of your API ID and API Key.
        // It should be in the format: base64(api_id:api_key)
        val apiKey = "NzM2YjBEYTRiMjRmY2ExNzBhNjViZjI4NTMzNDA=" // This is a placeholder. Replace with your actual Base64 encoded API key.
        val merchantId = "BNVWWLx" // This is your Hubtel POS Sales ID.

        val checkoutIntent = CheckoutIntent.Builder(this)
            .setAmount(amount)
            .setClientReference(clientReference)
            .setDescription("Payment for Tamaade Order")
            .setCustomerPhoneNumber(customerPhoneNumber) 
            .setCallbackUrl(callbackUrl)
            .setMerchantId(merchantId) 
            .setApiKey(apiKey) 
            .build()

        startActivityForResult(checkoutIntent, CHECKOUT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CHECKOUT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val status = data?.getParcelableExtra<CheckoutStatus?>(CheckoutStatus.CHECKOUT_RESULT)

                when {
                    status?.isPaymentSuccessful == true -> {
                        Log.d("CheckoutStatus", "Payment Successful. Transaction ID: ${status.transactionId}")
                        Toast.makeText(this, "Payment Successful!", Toast.LENGTH_LONG).show()
                        // TODO: Navigate to an Order Success screen
                        finish() // For now, just close the checkout
                    }
                    status?.isCanceled == true -> {
                        Log.d("CheckoutStatus", "Payment Canceled by user.")
                        Toast.makeText(this, "Payment Canceled.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.d("CheckoutStatus", "Payment Failed.")
                        Toast.makeText(this, "Payment Failed. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Log.d("CheckoutStatus", "Checkout was exited without completing.")
                Toast.makeText(this, "Payment process was not completed.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}