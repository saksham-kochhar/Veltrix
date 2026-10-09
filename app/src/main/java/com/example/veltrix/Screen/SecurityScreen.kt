package com.example.veltrix.Screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Password
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.veltrix.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.veltrix.Navigation.Routes
import com.example.veltrix.security.DeviceSession
import com.example.veltrix.security.LoginEvent
import com.example.veltrix.security.SecurityEvent
import com.example.veltrix.notifications.NotificationPreferencesRepository
import com.example.veltrix.security.SessionStore
import com.example.veltrix.veltrixviewmodel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PageBg = Color(0xFFF8F9FB)
private val Navy = Color(0xFF1E1B4B)
private val Muted = Color(0xFF94A3B8)
private val Accent = Color(0xFF534CFF)
private val Danger = Color(0xFFDC2626)

@Composable
fun SecurityHubScreen(
    navController: NavHostController,
    viewModel: veltrixviewmodel,
    onLinkGoogleClick: () -> Unit = {},
) {
    val hasPassword by viewModel.hasPasswordProvider.collectAsState()
    val hasGoogle by viewModel.hasGoogleProvider.collectAsState()
    val accountEmail by viewModel.accountEmail.collectAsState()
    val linkGoogleState by viewModel.linkGoogleState.collectAsState()
    var showSignOutAll by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val signOutAllState by viewModel.signOutAllState.collectAsState()

    var securityAlertsSubtitle by remember {
        mutableStateOf("Important changes to your account.")
    }

    LaunchedEffect(Unit) {
        viewModel.startSecurityListeners()
        runCatching {
            NotificationPreferencesRepository().fetchPreferences()
        }.onSuccess { prefs ->
            securityAlertsSubtitle = prefs.securityAlertsSubtitle()
        }
    }

    LaunchedEffect(linkGoogleState) {
        if (linkGoogleState is veltrixviewmodel.SecurityActionState.Success) {
            viewModel.clearLinkGoogleState()
        }
    }

    LaunchedEffect(signOutAllState) {
        when (val state = signOutAllState) {
            is veltrixviewmodel.SecurityActionState.Success -> {
                showSignOutAll = false
                viewModel.clearSecurityActionState()
                viewModel.signout()
                navController.navigate(Routes.Auth) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is veltrixviewmodel.SecurityActionState.Error -> Unit
            else -> Unit
        }
    }

    if (showSignOutAll) {
        AlertDialog(
            onDismissRequest = { showSignOutAll = false },
            title = { Text("Sign out of all devices?") },
            text = {
                Text("This will sign you out everywhere, including this device. You will need to sign in again.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.signOutAllDevices(context, keepCurrentDevice = false)
                    },
                    enabled = signOutAllState !is veltrixviewmodel.SecurityActionState.Loading
                ) { Text("Sign out everywhere", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutAll = false }) { Text("Cancel") }
            }
        )
    }

    SecurityScaffold(
        title = "Security",
        subtitle = "Sign-in, devices, and alerts.",
        navController = navController
    ) {
        SecuritySectionTitle("Password and sign-in")
        SecurityNavRow(
            title = "Change password",
            subtitle = if (hasPassword) {
                "Choose a strong password that you do not use elsewhere."
            } else {
                "You sign in with Google — manage password in your Google account."
            },
            icon = Icons.Outlined.Password,
            enabled = hasPassword,
            onClick = { navController.navigate(Routes.security_change_password) }
        )

        SecuritySectionTitle("Connected accounts")
        ConnectedAccountsCard(
            accountEmail = accountEmail,
            hasPassword = hasPassword,
            hasGoogle = hasGoogle,
            linkState = linkGoogleState,
            onLinkGoogleClick = onLinkGoogleClick,
        )

        SecuritySectionTitle("Security alerts")
        SecurityNavRow(
            title = "Recent security alerts",
            subtitle = securityAlertsSubtitle,
            icon = Icons.Outlined.NotificationsActive,
            onClick = { navController.navigate(Routes.security_alerts) }
        )

        SecuritySectionTitle("Devices and activity")
        SecurityNavRow(
            title = "Active sessions",
            subtitle = "Review where your account is signed in.",
            icon = Icons.Outlined.Devices,
            onClick = { navController.navigate(Routes.security_sessions) }
        )
        SecurityNavRow(
            title = "Login activity",
            subtitle = "Review recent sign-ins to your account.",
            icon = Icons.Outlined.History,
            onClick = { navController.navigate(Routes.security_login_history) }
        )
        SecurityNavRow(
            title = "Sign out of all devices",
            subtitle = "Sign out everywhere, then sign in again on this device.",
            icon = Icons.Outlined.Lock,
            titleColor = Danger,
            onClick = { showSignOutAll = true }
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ChangePasswordScreen(navController: NavHostController, viewModel: veltrixviewmodel) {
    val hasPassword by viewModel.hasPasswordProvider.collectAsState()
    val context = LocalContext.current
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var signOutOthers by remember { mutableStateOf(true) }
    val actionState by viewModel.changePasswordState.collectAsState()

    LaunchedEffect(actionState) {
        if (actionState is veltrixviewmodel.SecurityActionState.Success) {
            viewModel.clearChangePasswordState()
            navController.popBackStack()
        }
    }

    SecurityScaffold(
        title = "Change password",
        subtitle = "Confirm your current password, then choose a new one.",
        navController = navController
    ) {
        if (!hasPassword) {
            val ctx = LocalContext.current
            SecurityInfoCard("You sign in with Google. Open Google account settings to change your password.")
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_SYNC_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text("Open account settings") }
            return@SecurityScaffold
        }

        OutlinedTextField(
            value = current,
            onValueChange = { current = it },
            label = { Text("Current password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showCurrent = !showCurrent }) {
                    Text(if (showCurrent) "Hide" else "Show", fontSize = 12.sp)
                }
            },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showNew = !showNew }) {
                    Text(if (showNew) "Hide" else "Show", fontSize = 12.sp)
                }
            },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Text("At least 8 characters.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("Confirm new password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = signOutOthers, onCheckedChange = { signOutOthers = it })
            Text("Sign out other devices after changing password", fontSize = 13.sp)
        }

        if (actionState is veltrixviewmodel.SecurityActionState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                (actionState as veltrixviewmodel.SecurityActionState.Error).message,
                color = Danger,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.changePassword(context, current, newPassword, confirm, signOutOthers)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = actionState !is veltrixviewmodel.SecurityActionState.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            if (actionState is veltrixviewmodel.SecurityActionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
            } else {
                Text("Update password")
            }
        }
    }
}

@Composable
fun ActiveSessionsScreen(navController: NavHostController, viewModel: veltrixviewmodel) {
    val sessions by viewModel.deviceSessions.collectAsState()
    val context = LocalContext.current
    val currentSessionId = remember { SessionStore.getOrCreateSessionId(context) }
    val actionState by viewModel.revokeSessionState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshSecurityFromBackend() }

    SecurityScaffold(
        title = "Active sessions",
        subtitle = "Review where your account is signed in.",
        navController = navController
    ) {
        if (sessions.isEmpty()) {
            SecurityInfoCard("No active sessions recorded yet.")
        } else {
            sessions.forEach { session ->
                SessionRow(
                    session = session,
                    isCurrent = session.id == currentSessionId,
                    revoking = actionState is veltrixviewmodel.SecurityActionState.Loading,
                    onRevoke = {
                        if (session.id != currentSessionId) {
                            viewModel.revokeSession(session.id)
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        if (actionState is veltrixviewmodel.SecurityActionState.Error) {
            Text(
                (actionState as veltrixviewmodel.SecurityActionState.Error).message,
                color = Danger,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun LoginHistoryScreen(navController: NavHostController, viewModel: veltrixviewmodel) {
    val events by viewModel.loginEvents.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshSecurityFromBackend() }

    SecurityScaffold(
        title = "Login activity",
        subtitle = "Recent successful and failed sign-ins.",
        navController = navController
    ) {
        if (events.isEmpty()) {
            SecurityInfoCard("No login activity recorded yet.")
        } else {
            events.forEach { event ->
                LoginEventRow(event)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun SecurityAlertsScreen(navController: NavHostController, viewModel: veltrixviewmodel) {
    val alerts by viewModel.securityEvents.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshSecurityFromBackend() }

    SecurityScaffold(
        title = "Security alerts",
        subtitle = "Important changes to your account.",
        navController = navController
    ) {
        if (alerts.isEmpty()) {
            SecurityInfoCard("No security alerts yet.")
        } else {
            alerts.forEach { alert ->
                SecurityAlertRow(alert)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SecurityScaffold(
    title: String,
    subtitle: String,
    navController: NavHostController,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(28.dp)
                    .clickable { navController.popBackStack() },
                tint = Navy
            )
            Spacer(Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Navy)
        }
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 40.dp, top = 6.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SecuritySectionTitle(text: String) {
    Text(
        text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = Muted,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
    )
}

@Composable
private fun SecurityNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean = true,
    titleColor: Color = Navy,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (enabled) Accent else Muted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = if (enabled) titleColor else Muted)
                Text(subtitle, fontSize = 12.sp, color = Muted, lineHeight = 16.sp)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = Color(0xFFC8C8D4))
        }
    }
}

@Composable
private fun SecurityInfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(text, modifier = Modifier.padding(16.dp), color = Muted, fontSize = 14.sp)
    }
}

@Composable
private fun ConnectedAccountsCard(
    accountEmail: String?,
    hasPassword: Boolean,
    hasGoogle: Boolean,
    linkState: veltrixviewmodel.SecurityActionState,
    onLinkGoogleClick: () -> Unit,
) {
    val emailLine = accountEmail ?: "No email on this account"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            ConnectedAccountMethodRow(
                leading = { EmailProviderIcon() },
                title = "Email",
                subtitle = emailLine,
                trailing = {
                    if (hasPassword) {
                        ConnectedStatusBadge()
                    }
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE8ECF2)
            )
            ConnectedAccountMethodRow(
                leading = { GoogleProviderIcon() },
                title = "Google",
                subtitle = emailLine,
                trailing = {
                    when {
                        hasGoogle -> ConnectedStatusBadge()
                        linkState is veltrixviewmodel.SecurityActionState.Loading ->
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = Accent
                            )
                        hasPassword -> ConnectGoogleButton(onClick = onLinkGoogleClick)
                    }
                }
            )
            if (linkState is veltrixviewmodel.SecurityActionState.Error) {
                Text(
                    linkState.message,
                    color = Danger,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (linkState is veltrixviewmodel.SecurityActionState.Success) {
                Text(
                    "Google account connected.",
                    color = Color(0xFF16A34A),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectedAccountMethodRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Navy, fontSize = 15.sp)
            Text(subtitle, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun EmailProviderIcon() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Email,
            contentDescription = null,
            tint = Color(0xFFEA4335),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun GoogleProviderIcon() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = Color.Unspecified
        )
    }
}

@Composable
private fun ConnectedStatusBadge() {
    Surface(
        color = Color(0xFFECFDF3),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16A34A))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Connected",
                color = Color(0xFF16A34A),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ConnectGoogleButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent)
    ) {
        Icon(
            Icons.Outlined.Link,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text("Connect", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SessionRow(
    session: DeviceSession,
    isCurrent: Boolean,
    revoking: Boolean,
    onRevoke: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    session.deviceLabel.ifBlank { "Unknown device" },
                    fontWeight = FontWeight.SemiBold,
                    color = Navy
                )
                if (isCurrent) {
                    Surface(color = Color(0xFFE8F8EF), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "This device",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            }
            Text(session.platform.ifBlank { "android" }, fontSize = 12.sp, color = Muted)
            Text(
                formatTimestamp(session.lastActiveAt) ?: "Last active unknown",
                fontSize = 12.sp,
                color = Muted
            )
            session.approxLocation?.let {
                Text(it.displayLabel(), fontSize = 12.sp, color = Muted)
            }
            if (!session.isActive) {
                Text("Revoked", color = Danger, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            } else if (!isCurrent) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onRevoke,
                    enabled = !revoking
                ) { Text("Revoke", color = Danger) }
            }
        }
    }
}

@Composable
private fun LoginEventRow(event: LoginEvent) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (event.success) "Successful sign-in" else "Failed sign-in attempt",
                fontWeight = FontWeight.SemiBold,
                color = if (event.success) Navy else Danger
            )
            Text(
                formatTimestamp(event.timestamp) ?: "Unknown time",
                fontSize = 12.sp,
                color = Muted
            )
            Text(
                "Method: ${methodLabel(event.method)}",
                fontSize = 12.sp,
                color = Muted
            )
            event.deviceLabel?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 12.sp, color = Muted)
            }
            event.approxLocation?.let {
                Text(it.displayLabel(), fontSize = 12.sp, color = Muted)
            }
            if (!event.success && !event.failureReason.isNullOrBlank()) {
                Text(event.failureReason!!, fontSize = 12.sp, color = Danger)
            }
        }
    }
}

@Composable
private fun SecurityAlertRow(alert: SecurityEvent) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(alert.summary.ifBlank { alert.type }, fontWeight = FontWeight.SemiBold, color = Navy)
            Text(formatTimestamp(alert.timestamp) ?: "", fontSize = 12.sp, color = Muted)
        }
    }
}

private fun formatTimestamp(ts: com.google.firebase.Timestamp?): String? {
    ts ?: return null
    val fmt = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return fmt.format(Date(ts.seconds * 1000))
}

private fun methodLabel(method: String): String = when (method) {
    "google" -> "Google"
    "password" -> "Email & password"
    else -> method.ifBlank { "Unknown" }
}
