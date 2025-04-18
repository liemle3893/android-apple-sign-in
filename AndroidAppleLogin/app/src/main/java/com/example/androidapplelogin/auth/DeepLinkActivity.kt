package com.example.androidapplelogin.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.androidapplelogin.R
import com.example.androidapplelogin.databinding.ActivityDeepLinkBinding

/**
 * This activity helps users to manually input the authentication details
 * after completing Sign in with Apple in a web browser.
 *
 * When using a web redirect URI, the user will need to copy-paste the 
 * authorization code from the browser (or your server) to continue the auth flow.
 */
class DeepLinkActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDeepLinkBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeepLinkBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up the submit button
        binding.submitButton.setOnClickListener {
            processInput()
        }
        
        // Set up the cancel button
        binding.cancelButton.setOnClickListener {
            finish()
        }
    }
    
    private fun processInput() {
        val code = binding.authCodeEditText.text.toString().trim()
        val idToken = binding.idTokenEditText.text.toString().trim()
        
        if (code.isEmpty()) {
            binding.authCodeLayout.error = getString(R.string.auth_code_required)
            return
        }
        
        // Clear any errors
        binding.authCodeLayout.error = null
        binding.idTokenLayout.error = null
        
        // Show progress
        binding.progressBar.visibility = View.VISIBLE
        binding.submitButton.isEnabled = false
        binding.cancelButton.isEnabled = false
        
        // Notify the callback that we have auth details
        WebAuthCallback.notifyCallback(this, code, if (idToken.isEmpty()) null else idToken)
        
        // Let the user know it worked
        Toast.makeText(this, getString(R.string.sign_in_details_submitted), Toast.LENGTH_SHORT).show()
        
        // Finish this activity after a short delay
        binding.root.postDelayed({
            finish()
        }, 1000)
    }
} 
