package com.example.androidapplelogin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.androidapplelogin.auth.AppleAuthService
import com.example.androidapplelogin.databinding.ActivityUserProfileBinding
import com.example.androidapplelogin.model.User

/**
 * Activity that displays the user's profile information
 * after successful sign-in with Apple
 */
class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private lateinit var authService: AppleAuthService
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize auth service
        authService = AppleAuthService(this)

        // Get and display current user info
        loadUserData()

        // Set up log out button
        binding.logOutButton.setOnClickListener {
            logOut()
        }
    }

    private fun loadUserData() {
        // Get current user
        currentUser = authService.getCurrentUser()

        if (currentUser == null) {
            // No user found, go back to login screen
            Toast.makeText(
                this,
                getString(R.string.authentication_error),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        // Display user info
        displayUserInfo(currentUser!!)
    }

    private fun displayUserInfo(user: User) {
        // Set welcome text with name if available
        if (!user.fullName.isNullOrEmpty()) {
            binding.welcomeTextView.text = "${getString(R.string.welcome)} ${user.fullName}"
        }

        // Set user ID
        binding.userIdTextView.text = getString(R.string.user_id, user.id)

        // Set email (if available)
        binding.emailTextView.text = getString(
            R.string.email,
            user.email ?: "Private Email"
        )

        // Set name (if available)
        binding.nameTextView.text = getString(
            R.string.name,
            user.fullName ?: "Not Provided"
        )
    }

    private fun logOut() {
        // Clear all stored credentials
        authService.signOut()

        // Go back to login screen
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        authService.dispose()
    }
} 
