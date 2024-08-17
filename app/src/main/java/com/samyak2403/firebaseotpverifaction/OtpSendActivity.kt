/*
 * Created by Samyak kamble on 8/17/24, 2:50 PM
 *  Copyright (c) 2024 . All rights reserved.
 *  Last modified 8/17/24, 2:50 PM
 */

package com.samyak2403.firebaseotpverifaction

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.samyak2403.firebaseotpverifaction.databinding.ActivityOtpSendBinding

import java.util.concurrent.TimeUnit

class OtpSendActivity : AppCompatActivity() {

    // ViewBinding to access views in the layout
    private lateinit var binding: ActivityOtpSendBinding

    // Firebase Authentication instance
    private lateinit var mAuth: FirebaseAuth

    // Callback for phone authentication
    private lateinit var mCallbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpSendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance()

        // Set click listener on the Send button
        binding.btnSend.setOnClickListener {
            val phoneNumber = binding.etPhone.text.toString().trim()
            when {
                phoneNumber.isEmpty() -> {
                    Toast.makeText(this, "Invalid Phone Number", Toast.LENGTH_SHORT).show()
                }
                phoneNumber.length != 10 -> {
                    Toast.makeText(this, "Type valid Phone Number", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    otpSend()
                }
            }
        }
    }

    private fun otpSend() {
        // Show progress bar and hide the send button
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSend.visibility = View.INVISIBLE

        // Define the callback for handling verification events
        mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto verification completed, no need to enter the code manually
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // Hide progress bar and show the send button again on failure
                binding.progressBar.visibility = View.GONE
                binding.btnSend.visibility = View.VISIBLE
                Toast.makeText(this@OtpSendActivity, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // Hide progress bar and show the send button on success
                binding.progressBar.visibility = View.GONE
                binding.btnSend.visibility = View.VISIBLE
                Toast.makeText(this@OtpSendActivity, "OTP is successfully sent.", Toast.LENGTH_SHORT).show()

                // Navigate to OTP verification screen with the verification ID and phone number
                val intent = Intent(this@OtpSendActivity, OtpVerifyActivity::class.java).apply {
                    putExtra("phone", binding.etPhone.text.toString().trim())
                    putExtra("verificationId", verificationId)
                }
                startActivity(intent)
            }
        }

        // Set up the phone authentication options
        val options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber("+91" + binding.etPhone.text.toString().trim()) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout for verification
            .setActivity(this) // Activity to handle callback
            .setCallbacks(mCallbacks) // Callback defined earlier
            .build()

        // Start phone number verification
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}
