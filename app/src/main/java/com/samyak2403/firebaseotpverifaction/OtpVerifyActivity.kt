/*
 * Created by Samyak kamble on 8/17/24, 2:51 PM
 *  Copyright (c) 2024 . All rights reserved.
 *  Last modified 8/17/24, 2:51 PM
 */

package com.samyak2403.firebaseotpverifaction

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.registerReceiver
import androidx.core.content.ContextCompat.startActivity
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.samyak2403.firebaseotpverifaction.databinding.ActivityOtpVerifyBinding
import java.util.concurrent.TimeUnit


class OtpVerifyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpVerifyBinding // ViewBinding for accessing views
    private var verificationId: String? = null // To store the verification ID
    private var otpReceiver: OTPReceiver? = null // BroadcastReceiver for OTP auto-retrieval
    private lateinit var mAuth: FirebaseAuth // FirebaseAuth instance
    private lateinit var mCallbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks // Callbacks for OTP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        setupEditTextInput()

        // Set phone number from Intent data
        binding.tvMobile.text = String.format("+91-%s", intent.getStringExtra("phone"))

        verificationId = intent.getStringExtra("verificationId")

        // Resend OTP on click
        binding.tvResendBtn.setOnClickListener {
            resendOtp()
        }

        // Verify OTP on button click
        binding.btnVerify.setOnClickListener {
            binding.progressBarVerify.visibility = View.VISIBLE
            binding.btnVerify.visibility = View.INVISIBLE
            if (isOtpValid()) {
                verificationId?.let { id ->
                    val code = getOtpCode()
                    val credential = PhoneAuthProvider.getCredential(id, code)
                    signInWithCredential(credential)
                }
            } else {
                Toast.makeText(this, "OTP is not Valid!", Toast.LENGTH_SHORT).show()
            }
        }

        // Start auto OTP retrieval
        startAutoOtpReceiver()
    }

    // Check if all OTP fields are filled
    private fun isOtpValid(): Boolean {
        return with(binding) {
            listOf(etC1, etC2, etC3, etC4, etC5, etC6).none { it.text.toString().trim().isEmpty() }
        }
    }

    // Get concatenated OTP code from all input fields
    private fun getOtpCode(): String {
        return with(binding) {
            etC1.text.toString().trim() +
                    etC2.text.toString().trim() +
                    etC3.text.toString().trim() +
                    etC4.text.toString().trim() +
                    etC5.text.toString().trim() +
                    etC6.text.toString().trim()
        }
    }

    // Sign in with PhoneAuthCredential
    private fun signInWithCredential(credential: PhoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    binding.progressBarVerify.visibility = View.VISIBLE
                    binding.btnVerify.visibility = View.INVISIBLE
                    Toast.makeText(this, "Welcome...", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } else {
                    binding.progressBarVerify.visibility = View.GONE
                    binding.btnVerify.visibility = View.VISIBLE
                    Toast.makeText(this, "OTP is not Valid!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Start SMS Retriever to auto-detect OTP
    private fun startAutoOtpReceiver() {
        otpReceiver = OTPReceiver().also { receiver ->
            registerReceiver(receiver, IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION))
            receiver.initListener(object : OTPReceiver.OtpReceiverListener {
                override fun onOtpSuccess(otp: String) {
                    // Set OTP in text fields automatically
                    binding.etC1.setText(otp[0].toString())
                    binding.etC2.setText(otp[1].toString())
                    binding.etC3.setText(otp[2].toString())
                    binding.etC4.setText(otp[3].toString())
                    binding.etC5.setText(otp[4].toString())
                    binding.etC6.setText(otp[5].toString())
                }

                override fun onOtpTimeout() {
                    Toast.makeText(this@OtpVerifyActivity, "Something went wrong!", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // Resend OTP when the user clicks "Resend"
    private fun resendOtp() {
        binding.progressBarVerify.visibility = View.VISIBLE
        binding.btnVerify.visibility = View.INVISIBLE

        mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-verification success
            }

            override fun onVerificationFailed(e: FirebaseException) {
                binding.progressBarVerify.visibility = View.GONE
                binding.btnVerify.visibility = View.VISIBLE
                Toast.makeText(this@OtpVerifyActivity, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                binding.progressBarVerify.visibility = View.GONE
                binding.btnVerify.visibility = View.VISIBLE
                Toast.makeText(this@OtpVerifyActivity, "OTP is successfully sent.", Toast.LENGTH_SHORT).show()
            }
        }

        val options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber("+91" + intent.getStringExtra("phone")!!.trim()) // Set phone number
            .setTimeout(60L, TimeUnit.SECONDS) // Set timeout duration
            .setActivity(this) // Set the current activity
            .setCallbacks(mCallbacks) // Set callbacks
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // Handle focus change as the user types OTP digits
    private fun setupEditTextInput() {
        binding.etC1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etC2.requestFocus()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etC2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etC3.requestFocus()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etC3.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etC4.requestFocus()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etC4.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etC5.requestFocus()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etC5.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etC6.requestFocus()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Unregister the OTP receiver to avoid memory leaks
    override fun onDestroy() {
        super.onDestroy()
        otpReceiver?.let {
            unregisterReceiver(it)
        }
    }
}
