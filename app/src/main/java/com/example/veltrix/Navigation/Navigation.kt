package com.example.veltrix.Navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.veltrix.Screen.AccountScreen
import com.example.veltrix.Screen.AuthScreen
import com.example.veltrix.Screen.ChatbotScreen
import com.example.veltrix.Screen.PlanSelectionScreen
import com.example.veltrix.Screen.ProfileInformationScreen
import com.example.veltrix.Screen.VerificationScreen
import com.example.veltrix.Screen.ActiveSessionsScreen
import com.example.veltrix.Screen.ChangePasswordScreen
import com.example.veltrix.Screen.LoginHistoryScreen
import com.example.veltrix.Screen.SecurityAlertsScreen
import com.example.veltrix.Screen.NotificationsScreen
import com.example.veltrix.Screen.SecurityHubScreen
import com.example.veltrix.Screen.OfflineDataScreen
import com.example.veltrix.Screen.HelpSupportScreen
import com.example.veltrix.veltrixviewmodel

@Composable
fun navigation(
    viewModel: veltrixviewmodel,
    onGoogleSignIn: () -> Unit,
    onLinkGoogle: () -> Unit,
) {
    val navController = rememberNavController()

    LaunchedEffect(viewModel.pendingNotificationRoute) {
        val (route, _) = viewModel.consumePendingNotificationRoute()
        when (route) {
            "security_alerts" -> navController.navigate(Routes.security_alerts)
            "account" -> navController.navigate(Routes.account)
            "chat", "home" -> navController.navigate(Routes.Home)
            else -> Unit
        }
    }
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var forgotMessage by remember { mutableStateOf<String?>(null) }
    var forgotLoading by remember { mutableStateOf(false) }

    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = {
                if (!forgotLoading) showForgotPassword = false
            },
            title = { Text("Reset password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    forgotMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        forgotLoading = true
                        forgotMessage = null
                        viewModel.requestPasswordReset(forgotEmail) { result ->
                            forgotLoading = false
                            result.onSuccess {
                                forgotMessage =
                                    "If an account exists for this email, you will receive reset instructions shortly."
                            }.onFailure {
                                forgotMessage =
                                    "If an account exists for this email, you will receive reset instructions shortly."
                            }
                        }
                    },
                    enabled = !forgotLoading && forgotEmail.isNotBlank()
                ) { Text(if (forgotLoading) "Sending…" else "Send link") }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPassword = false }) { Text("Close") }
            }
        )
    }

    NavHost(navController, startDestination = Routes.Auth) {

        composable(Routes.Auth) {
            AuthScreen(
                viewModel = viewModel,
                navController = navController,
                onGoogleClick = onGoogleSignIn,
                onForgotPassword = { email ->
                    forgotEmail = email
                    forgotMessage = null
                    showForgotPassword = true
                }
            )
        }
        composable(Routes.Home) {
            ChatbotScreen(viewmodel = viewModel, navController)
        }
        composable(Routes.verification) {
            VerificationScreen(viewModel = viewModel, navController)
        }
        composable(Routes.account) {
            AccountScreen(navController, viewModel)
        }
        composable(Routes.profile) {
            ProfileInformationScreen(navController, viewModel)
        }
        composable(Routes.details) {
            PlanSelectionScreen(viewModel, navController)
        }
        composable(Routes.notifications) {
            NotificationsScreen(navController = navController)
        }
        composable(Routes.offlineData) {
            OfflineDataScreen(navController = navController, viewModel = viewModel)
        }
        composable(Routes.helpSupport) {
            HelpSupportScreen(navController = navController, viewModel = viewModel)
        }
        composable(Routes.security) {
            SecurityHubScreen(
                navController = navController,
                viewModel = viewModel,
                onLinkGoogleClick = onLinkGoogle,
            )
        }
        composable(Routes.security_change_password) {
            ChangePasswordScreen(navController, viewModel)
        }
        composable(Routes.security_sessions) {
            ActiveSessionsScreen(navController, viewModel)
        }
        composable(Routes.security_login_history) {
            LoginHistoryScreen(navController, viewModel)
        }
        composable(Routes.security_alerts) {
            SecurityAlertsScreen(navController, viewModel)
        }
    }
}
