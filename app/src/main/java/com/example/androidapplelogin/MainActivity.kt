package com.example.androidapplelogin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.androidapplelogin.auth.AppleAuthService
import com.example.androidapplelogin.auth.AuthResult
import com.example.androidapplelogin.auth.DeepLinkActivity
import com.example.androidapplelogin.auth.WebAuthCallback
import com.example.androidapplelogin.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Main activity serving as the entry point of the app
 * Presents a Sign in with Apple button for authentication
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authService: AppleAuthService
    private lateinit var authLauncher: ActivityResultLauncher<Intent>
    private lateinit var webAuthCallback: WebAuthCallback

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize auth service
        authService = AppleAuthService(this)
        
        // Initialize web auth callback
        webAuthCallback = WebAuthCallback(this)

        // Register for activity result
        authLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // The actual result will come through the WebAuthCallback
            Log.d(TAG, "Browser finished with result code: ${result.resultCode}")
            // Hide loading indicator as the browser has closed
            binding.progressBar.visibility = View.GONE
        }

        // Set up the Sign in with Apple button
        binding.appleSignInButton.setOnClickListener {
            initiateAppleSignIn()
        }
        
        // Set up the help button for manual code entry
        binding.helpButton.setOnClickListener {
            openManualCodeEntry()
        }

        // Check if user is already logged in
        checkAuthStatus()
    }
    
    override fun onResume() {
        super.onResume()
        // Hide loading indicator when returning to this activity
        binding.progressBar.visibility = View.GONE
        
        // Set up the callback for web authentication result
        webAuthCallback.registerAuthCallback { code, idToken, error ->
            if (error != null) {
                // Handle error
                Toast.makeText(
                    this,
                    getString(R.string.authentication_error) + ": $error",
                    Toast.LENGTH_SHORT
                ).show()
                return@registerAuthCallback
            }
            
            if (code == null) {
                // No authorization code received
                Toast.makeText(
                    this,
                    getString(R.string.sign_in_failed),
                    Toast.LENGTH_SHORT
                ).show()
                return@registerAuthCallback
            }
            
            // Process the authorization code
            processAuthResult(code, idToken)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Unregister the callback to prevent leaks
        webAuthCallback.unregisterAuthCallback()
    }

    private fun checkAuthStatus() {
        if (authService.isUserLoggedIn()) {
            // User is already logged in, go directly to profile screen
            val profileIntent = Intent(this, UserProfileActivity::class.java)
            startActivity(profileIntent)
            // Don't finish this activity so user can return to it when logging out
        }
    }

    private fun initiateAppleSignIn() {
        try {
            // Show loading indicator
            binding.progressBar.visibility = View.VISIBLE

            // Get authorization request intent from auth service
            val authIntent = authService.getAuthorizationRequestIntent()
            
            // Launch the authorization flow in Custom Tabs browser
            authLauncher.launch(authIntent)
            
            // Let the user know they need to sign in through the browser
            Toast.makeText(
                this,
                getString(R.string.browser_sign_in_message),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating Apple sign-in", e)
            Toast.makeText(
                this,
                getString(R.string.authentication_error),
                Toast.LENGTH_SHORT
            ).show()
            
            // Hide loading indicator
            binding.progressBar.visibility = View.GONE
        }
    }
    
    private fun processAuthResult(code: String, idToken: String?) {
        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Create an intent with the auth code to process
                val intent = Intent().apply {
                    putExtra("code", code)
                    if (idToken != null) {
                        putExtra("id_token", idToken)
                    }
                }
                
                // Process the authentication
                val result = authService.handleAuthorizationResponse(intent)
                
                when (result) {
                    is AuthResult.Success -> {
                        Log.d(TAG, "Successfully authenticated with Apple: ${result.user.id}")
                        
                        // Navigate to user profile activity
                        val profileIntent = Intent(this@MainActivity, UserProfileActivity::class.java)
                        startActivity(profileIntent)
                    }
                    is AuthResult.Error -> {
                        Log.e(TAG, "Authentication error: ${result.message}")
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.sign_in_failed) + ": ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                
                // Hide loading indicator
                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "Error processing auth result", e)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.authentication_error),
                    Toast.LENGTH_SHORT
                ).show()
                
                // Hide loading indicator
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun openManualCodeEntry() {
        val intent = Intent(this, DeepLinkActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        authService.dispose()
    }
} 
