package com.example.cherryai

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.cherryai.notifications.AppForegroundHolder
import com.example.cherryai.Navigation.navigation
import com.example.cherryai.ui.theme.CherryAITheme
import com.example.cherryai.ui.theme.ThemeMode
import com.example.cherryai.ui.theme.ThemePreferences
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewmodel: cherryAiViewModel

    companion object {
        private const val TAG = "MainActivity"
        const val EXTRA_NOTIFICATION_ROUTE = "notification_route"
        const val EXTRA_SESSION_ID = "session_id"
    }

    // Web client ID (type 3) from google-services.json
    private val webClientId =
        "786319689935-2rrh7gdms6p8lol3occs4dbdou1capha.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewmodel = ViewModelProvider(this)[cherryAiViewModel::class.java]
        viewmodel.attachProfileStorage(applicationContext)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                AppForegroundHolder.isForeground = true
            }

            override fun onStop(owner: LifecycleOwner) {
                AppForegroundHolder.isForeground = false
            }
        })
        viewmodel.handleNotificationIntent(intent)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember {
                mutableStateOf(ThemePreferences.getMode(applicationContext))
            }
            CherryAITheme(themeMode = themeMode) {
                navigation(
                    viewmodel,
                    themeMode = themeMode,
                    onThemeModeChanged = { themeMode = it },
                    onGoogleSignIn = { launchGoogleSignIn(viewmodel, GoogleSignInMode.SignIn) },
                    onLinkGoogle = { launchGoogleSignIn(viewmodel, GoogleSignInMode.LinkToCurrentAccount) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewmodel.handleNotificationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewmodel.registerPushIfNeeded(applicationContext)
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (!user.isEmailVerified && user.providerData.any {
                it.providerId == com.google.firebase.auth.EmailAuthProvider.PROVIDER_ID
            }
        ) {
            return
        }
        lifecycleScope.launch {
            runCatching {
                if (viewmodel.syncSecuritySession(this@MainActivity)) {
                    viewmodel.signout()
                    return@runCatching
                }
                viewmodel.startSecurityListeners()
            }.onFailure { error ->
                Log.w(TAG, "Security session sync skipped", error)
            }
        }
    }

    private fun launchGoogleSignIn(viewmodel: cherryAiViewModel, mode: GoogleSignInMode) {
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
                    viewmodel.handleGoogleIdToken(googleCredential.idToken, mode)
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
