package com.example.androidapplelogin.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.androidapplelogin.R
import com.example.androidapplelogin.UserProfileActivity
import com.example.androidapplelogin.databinding.ActivityAppleSignInBinding
import kotlinx.coroutines.launch

/**
 * Activity that handles the callback from Apple's sign-in flow
 */
class AppleSignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppleSignInBinding
    private lateinit var authService: AppleAuthService

    companion object {
        private const val TAG = "AppleSignInActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppleSignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize auth service
        authService = AppleAuthService(this)

        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE
        binding.statusTextView.text = getString(R.string.loading)

        // Handle the intent
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // Process the intent in a coroutine to avoid blocking the UI thread
        lifecycleScope.launch {
            try {
                val result = authService.handleAuthorizationResponse(intent)
                
                when (result) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "Successfully authenticated with Apple: ${result.user.id}")
                        
                        // Navigate to user profile activity
                        val profileIntent = Intent(this@AppleSignInActivity, UserProfileActivity::class.java)
                        startActivity(profileIntent)
                        finish() // Close this activity
                    }
                    is AuthResult.Error -> {
                        Log.e(TAG, "Authentication error: ${result.message}")
                        showError(result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing authorization response", e)
                showError("Error: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.statusTextView.text = getString(R.string.sign_in_failed)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        // Go back to login screen after a short delay
        binding.root.postDelayed({
            finish()
        }, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        authService.dispose()
    }
} 
