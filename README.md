# Android App with Sign In with Apple

This Android application demonstrates how to implement "Sign In with Apple" functionality in an Android app. It uses OAuth 2.0 and OpenID Connect to authenticate users with their Apple ID.

## Features

- Sign In with Apple button that complies with Apple's design guidelines
- Secure authentication using OAuth 2.0 and OpenID Connect
- User profile display with information from Apple ID
- Secure token storage using Android's EncryptedSharedPreferences
- Support for private relay emails
- JWT validation
- Proper error handling

## Setup Instructions

### 1. Apple Developer Account Setup

1. Sign in to your [Apple Developer Account](https://developer.apple.com/)
2. Create a new App ID in Certificates, Identifiers & Profiles
3. Enable "Sign In with Apple" capability for this App ID
4. Create a Services ID (this will be your CLIENT_ID)
5. Configure the Services ID:
   - Enable "Sign In with Apple"
   - Add your domain to the list of domains
   - Set the Return URL to match your REDIRECT_URI

### 2. Project Configuration

1. Open the project in Android Studio
2. Update the `CLIENT_ID` in `AppleAuthService.kt` with your Services ID from Apple Developer Console
3. Ensure the `REDIRECT_URI` matches what you configured in the Apple Developer Console
4. Build and run the application

### 3. Security Considerations

- The sample app uses EncryptedSharedPreferences for secure storage
- JWT validation ensures the token integrity
- In a production app, you should verify tokens on your backend server

## Project Structure

- `MainActivity`: Entry point with Sign In with Apple button
- `AppleAuthService`: Core service handling the authentication flow
- `AppleSignInActivity`: Handles OAuth callback
- `UserProfileActivity`: Displays user info after successful sign-in
- `SecureStorageManager`: Handles secure storage of tokens and user data
- `User`: Data model for user information

## Dependencies

- AppAuth for OAuth/OIDC: `net.openid:appauth:0.11.1`
- JWT Decoding: `com.auth0.android:jwtdecode:2.0.1`
- Secure Storage: `androidx.security:security-crypto:1.1.0-alpha06`

## Implementation Notes

- The app uses AppAuth library for OAuth 2.0 and OpenID Connect implementation
- Custom tabs are used for the authentication flow
- The app supports both regular and private relay emails
- Token validation is performed to ensure security

## Troubleshooting

- Make sure your Apple Developer Account is properly configured
- Check that the CLIENT_ID matches your Services ID
- Verify that the REDIRECT_URI is correctly set in both the app and Apple Developer Console
- If you're testing on an emulator, ensure it has Google Play Services installed

## Further Reading

- [Sign In with Apple REST API](https://developer.apple.com/documentation/sign_in_with_apple/sign_in_with_apple_rest_api)
- [AppAuth for Android](https://github.com/openid/AppAuth-Android)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices) 
