package com.example.androidapplelogin.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.auth0.android.jwt.JWT
import com.example.androidapplelogin.model.User
import com.example.androidapplelogin.utils.SecureStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Service responsible for handling Apple authentication flow
 */
class AppleAuthService(private val context: Context) {

    companion object {
        private const val TAG = "AppleAuthService"
        
        // Apple Auth endpoints
        private const val AUTHORIZATION_ENDPOINT = "https://appleid.apple.com/auth/authorize"
        private const val TOKEN_ENDPOINT = "https://appleid.apple.com/auth/token"
        
        // OAuth parameters
        private const val TEAM_ID = "9568FP2WHH"
        private const val CLIENT_ID = "io.nunchuk.signin" // Service ID from Apple Developer Account
        private const val REDIRECT_URI = "https://api.nunchuk.io/v1.1/passport/apple/signin_callback"
        private const val SCOPE = "name email"
        
        // Service configuration
        private val serviceConfiguration = AuthorizationServiceConfiguration(
            Uri.parse(AUTHORIZATION_ENDPOINT),
            Uri.parse(TOKEN_ENDPOINT)
        )
    }

    private val authService = AuthorizationService(context)
    private val secureStorageManager = SecureStorageManager(context)

    /**
     * Creates an intent for Apple sign-in using Custom Tabs
     * This is necessary because we're using a web redirect URI
     */
    fun getAuthorizationRequestIntent(): Intent {
        val authRequestBuilder = AuthorizationRequest.Builder(
            serviceConfiguration,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(REDIRECT_URI)
        )
        
        // Configure the request
        authRequestBuilder
            .setScope(SCOPE)
            .setResponseMode(ResponseModeValues.FORM_POST)
            .setAdditionalParameters(
                mapOf(
                    "response_mode" to "form_post",
                    "team_id" to TEAM_ID
                )
            )
        
        val authRequest = authRequestBuilder.build()
        
        // Create the authorization service
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            
        return authService.getAuthorizationRequestIntent(
            authRequest,
            customTabsIntent
        )
    }

    /**
     * Processes the response from Apple's authorization endpoint
     * For web redirects, we'll pass in a manually constructed intent with the auth code
     */
    suspend fun handleAuthorizationResponse(intent: Intent): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check if we're receiving the code directly (from our web callback)
                val code = intent.getStringExtra("code")
                val idToken = intent.getStringExtra("id_token")
                
                // If we have a code directly, create a mock response
                if (code != null) {
                    Log.d(TAG, "Processing direct authorization code")
                    
                    // If we have the ID token directly, we can extract user info
                    if (idToken != null) {
                        Log.d(TAG, "ID token provided directly, extracting user info")
                        val user = extractUserFromIdToken(idToken)
                        
                        // Save the tokens
                        secureStorageManager.saveIdToken(idToken)
                        secureStorageManager.saveUserId(user.id)
                        secureStorageManager.saveEmail(user.email)
                        secureStorageManager.saveFirstName(user.firstName)
                        secureStorageManager.saveLastName(user.lastName)
                        
                        return@withContext AuthResult.Success(user)
                    }
                    
                    // Otherwise, we need to exchange the code for tokens
                    Log.d(TAG, "Exchanging authorization code for tokens")
                    
                    // Create a manual AuthorizationResponse to exchange the code
                    val serviceConfig = AuthorizationServiceConfiguration(
                        Uri.parse(AUTHORIZATION_ENDPOINT),
                        Uri.parse(TOKEN_ENDPOINT)
                    )
                    
                    val authRequest = AuthorizationRequest.Builder(
                        serviceConfig,
                        CLIENT_ID,
                        ResponseTypeValues.CODE,
                        Uri.parse(REDIRECT_URI)
                    ).build()
                    
                    val authResponse = AuthorizationResponse.Builder(authRequest)
                        .setAuthorizationCode(code)
                        .build()
                    
                    // Exchange code for tokens
                    val tokenResult = exchangeAuthorizationCode(authResponse)
                    
                    if (tokenResult is TokenResult.Success) {
                        // Parse user data from ID token
                        val user = extractUserFromIdToken(tokenResult.idToken)
                        
                        // Save tokens and user data
                        secureStorageManager.saveIdToken(tokenResult.idToken)
                        tokenResult.accessToken?.let { secureStorageManager.saveAccessToken(it) }
                        tokenResult.refreshToken?.let { secureStorageManager.saveRefreshToken(it) }
                        
                        secureStorageManager.saveUserId(user.id)
                        secureStorageManager.saveEmail(user.email)
                        secureStorageManager.saveFirstName(user.firstName)
                        secureStorageManager.saveLastName(user.lastName)
                        
                        return@withContext AuthResult.Success(user)
                    } else {
                        return@withContext AuthResult.Error((tokenResult as TokenResult.Error).message)
                    }
                } else {
                    // This is the standard AppAuth flow response
                    val response = AuthorizationResponse.fromIntent(intent)
                    val exception = AuthorizationException.fromIntent(intent)
                    
                    if (exception != null) {
                        Log.e(TAG, "Authorization error: ${exception.message}")
                        return@withContext AuthResult.Error(exception.message ?: "Authorization failed")
                    }
                    
                    if (response == null) {
                        Log.e(TAG, "Authorization response is null")
                        return@withContext AuthResult.Error("Authorization response is null")
                    }
                    
                    // Exchange authorization code for tokens
                    val tokenResponse = exchangeAuthorizationCode(response)
                    if (tokenResponse is TokenResult.Success) {
                        // Parse user data from ID token
                        val user = extractUserFromIdToken(tokenResponse.idToken)
                        
                        // Save tokens and user data
                        secureStorageManager.saveIdToken(tokenResponse.idToken)
                        tokenResponse.accessToken?.let { secureStorageManager.saveAccessToken(it) }
                        tokenResponse.refreshToken?.let { secureStorageManager.saveRefreshToken(it) }
                        
                        secureStorageManager.saveUserId(user.id)
                        secureStorageManager.saveEmail(user.email)
                        secureStorageManager.saveFirstName(user.firstName)
                        secureStorageManager.saveLastName(user.lastName)
                        
                        return@withContext AuthResult.Success(user)
                    } else {
                        return@withContext AuthResult.Error((tokenResponse as TokenResult.Error).message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling authorization response", e)
                return@withContext AuthResult.Error("Error handling authorization: ${e.message}")
            }
        }
    }

    /**
     * Exchanges authorization code for tokens
     */
    private suspend fun exchangeAuthorizationCode(authResponse: AuthorizationResponse): TokenResult {
        return withContext(Dispatchers.IO) {
            try {
                val tokenRequest = authResponse.createTokenExchangeRequest()
                val response = performTokenRequest(tokenRequest)
                
                if (response?.accessToken != null) {
                    return@withContext TokenResult.Success(
                        idToken = response.idToken ?: "",
                        accessToken = response.accessToken,
                        refreshToken = response.refreshToken
                    )
                } else {
                    return@withContext TokenResult.Error("Failed to retrieve tokens")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exchanging authorization code", e)
                return@withContext TokenResult.Error("Error exchanging authorization code: ${e.message}")
            }
        }
    }

    /**
     * Performs token request using AppAuth
     */
    private suspend fun performTokenRequest(request: TokenRequest): TokenResponse? {
        return withContext(Dispatchers.IO) {
            try {
                var tokenResponse: TokenResponse? = null
                val latch = java.util.concurrent.CountDownLatch(1)
                
                authService.performTokenRequest(request) { response, exception ->
                    if (exception != null) {
                        Log.e(TAG, "Token exchange error", exception)
                    } else {
                        tokenResponse = response
                    }
                    latch.countDown()
                }
                
                latch.await()
                return@withContext tokenResponse
            } catch (e: Exception) {
                Log.e(TAG, "Error performing token request", e)
                return@withContext null
            }
        }
    }

    /**
     * Extracts user information from the ID token
     */
    private fun extractUserFromIdToken(idToken: String): User {
        try {
            val jwt = JWT(idToken)
            
            // Extract user ID (sub claim)
            val userId = jwt.getClaim("sub").asString() ?: ""
            
            // Extract email
            val email = jwt.getClaim("email").asString()
            
            // Extract name (if available)
            var firstName: String? = null
            var lastName: String? = null
            
            // Try to get name from the name claim (may or may not be present)
            try {
                val fullNameJson = jwt.getClaim("name").asObject(HashMap::class.java)
                if (fullNameJson != null) {
                    firstName = fullNameJson["firstName"] as? String
                    lastName = fullNameJson["lastName"] as? String
                }
            } catch (e: Exception) {
                Log.d(TAG, "Name claim not found or not in expected format")
            }
            
            return User(
                id = userId,
                email = email,
                firstName = firstName,
                lastName = lastName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing ID token", e)
            // Return a user with just the user ID in case parsing fails
            return User(
                id = UUID.randomUUID().toString(), // Fallback ID
                email = null,
                firstName = null,
                lastName = null
            )
        }
    }

    /**
     * Signs out the current user by clearing stored credentials
     */
    fun signOut() {
        secureStorageManager.clearAllData()
    }

    /**
     * Checks if a user is currently logged in
     */
    fun isUserLoggedIn(): Boolean {
        return secureStorageManager.isUserLoggedIn()
    }

    /**
     * Retrieves the current user from secure storage
     */
    fun getCurrentUser(): User? {
        val userId = secureStorageManager.getUserId() ?: return null
        
        return User(
            id = userId,
            email = secureStorageManager.getEmail(),
            firstName = secureStorageManager.getFirstName(),
            lastName = secureStorageManager.getLastName()
        )
    }

    /**
     * Performs token validation (in a real app, this should be done on your backend)
     */
    fun validateIdToken(idToken: String): Boolean {
        try {
            val jwt = JWT(idToken)
            
            // Check token expiration
            if (jwt.isExpired(0)) {
                Log.e(TAG, "ID token is expired")
                return false
            }
            
            // Check issuer
            val issuer = jwt.issuer
            if (issuer != "https://appleid.apple.com") {
                Log.e(TAG, "Invalid token issuer: $issuer")
                return false
            }
            
            // Check audience (should be your client ID)
            val audience = jwt.audience
            if (!audience.contains(CLIENT_ID)) {
                Log.e(TAG, "Invalid token audience: $audience")
                return false
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating ID token", e)
            return false
        }
    }

    /**
     * Clean up resources when service is no longer needed
     */
    fun dispose() {
        authService.dispose()
    }
}

/**
 * Sealed class representing the result of token exchange
 */
sealed class TokenResult {
    data class Success(
        val idToken: String,
        val accessToken: String? = null,
        val refreshToken: String? = null
    ) : TokenResult()
    
    data class Error(val message: String) : TokenResult()
}

/**
 * Sealed class representing the result of authentication
 */
sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
} 
