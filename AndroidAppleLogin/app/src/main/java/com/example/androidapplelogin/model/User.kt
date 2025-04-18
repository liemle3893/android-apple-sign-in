package com.example.androidapplelogin.model

/**
 * Data class representing a user after successful authentication with Apple
 */
data class User(
    val id: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?
) {
    val fullName: String?
        get() = if (firstName != null || lastName != null) {
            listOfNotNull(firstName, lastName).joinToString(" ")
        } else {
            null
        }
} 
