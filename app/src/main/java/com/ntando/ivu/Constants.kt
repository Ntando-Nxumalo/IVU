package com.ntando.ivu

/**
 * [Constants] holds application-wide immutable constant values.
 *
 * Properties:
 * - [WEB_CLIENT_ID]: The OAuth 2.0 Web Client ID registered in Google Cloud Console / Firebase Console.
 *   This client ID is required by [androidx.credentials.CredentialManager] when requesting Google ID Tokens
 *   for authentication via [com.google.android.libraries.identity.googleid.GetGoogleIdOption].
 */
object Constants {
    /**
     * Default OAuth Web Client ID for Google Sign-In and Credential Manager integration.
     */
    const val WEB_CLIENT_ID = "34465820973-avb1jfir18fi7qi5eoatv9anri6qus7e.apps.googleusercontent.com"
}
