package com.example.androidapplelogin.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * This class will help handle the web-based authentication callback
 * by providing a mechanism to notify the app when a manual callback occurs
 */
class WebAuthCallback(private val context: Context) {
    
    companion object {
        private const val TAG = "WebAuthCallback"
        const val ACTION_AUTH_CALLBACK = "com.example.androidapplelogin.AUTH_CALLBACK"
        const val EXTRA_AUTH_CODE = "auth_code"
        const val EXTRA_ID_TOKEN = "id_token"
        const val EXTRA_ERROR = "error"
        
        /**
         * Call this method from a callback activity or receiver to notify
         * that authentication has completed
         */
        fun notifyCallback(context: Context, code: String?, idToken: String?, error: String? = null) {
            val intent = Intent(ACTION_AUTH_CALLBACK).apply {
                putExtra(EXTRA_AUTH_CODE, code)
                putExtra(EXTRA_ID_TOKEN, idToken)
                if (error != null) {
                    putExtra(EXTRA_ERROR, error)
                }
            }
            
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            Log.d(TAG, "Notified authentication callback: code=${code != null}, idToken=${idToken != null}, error=$error")
        }
    }
    
    // Broadcast receiver to get notified when auth completes
    private var authCallbackReceiver: BroadcastReceiver? = null
    
    /**
     * Register a callback to be notified when authentication completes
     */
    fun registerAuthCallback(onResult: (code: String?, idToken: String?, error: String?) -> Unit) {
        // First unregister any existing receiver
        unregisterAuthCallback()
        
        // Create and register new receiver
        authCallbackReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val code = intent.getStringExtra(EXTRA_AUTH_CODE)
                val idToken = intent.getStringExtra(EXTRA_ID_TOKEN)
                val error = intent.getStringExtra(EXTRA_ERROR)
                
                Log.d(TAG, "Received auth callback: code=${code != null}, idToken=${idToken != null}, error=$error")
                onResult(code, idToken, error)
            }
        }
        
        LocalBroadcastManager.getInstance(context).registerReceiver(
            authCallbackReceiver!!,
            IntentFilter(ACTION_AUTH_CALLBACK)
        )
    }
    
    /**
     * Unregister the auth callback when no longer needed
     */
    fun unregisterAuthCallback() {
        authCallbackReceiver?.let {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(it)
            authCallbackReceiver = null
        }
    }
} 
