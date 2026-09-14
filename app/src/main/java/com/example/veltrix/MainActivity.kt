package com.example.veltrix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.veltrix.Navigation.navigation
import com.example.veltrix.ui.theme.VeltrixTheme
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Web client ID (type 3) from google-services.json
    private val webClientId =
        "786319689935-2rrh7gdms6p8lol3occs4dbdou1capha.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        val viewmodel = ViewModelProvider(this)[veltrixviewmodel::class.java]
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeltrixTheme {
                navigation(viewmodel, onGoogleClick = { launchGoogleSignIn(viewmodel) })
            }
        }
    }

    private fun launchGoogleSignIn(viewmodel: veltrixviewmodel) {
        val credentialManager = CredentialManager.create(this)

        // GetSignInWithGoogleOption always shows the account picker bottom sheet,
        // unlike GetGoogleIdOption which fails with NoCredentialException when
        // no prior authorized session exists for this app.
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@MainActivity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    viewmodel.signInWithGoogle(googleCredential.idToken)
                } else {
                    viewmodel.setError("Unexpected credential type. Try again.")
                }
            } catch (_: GetCredentialCancellationException) {
                // User dismissed the picker — nothing to do
            } catch (e: Exception) {
                viewmodel.setError("Google sign-in failed: ${e.message}")
            }
        }
    }
}
