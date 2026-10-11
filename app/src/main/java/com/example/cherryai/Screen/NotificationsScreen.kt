package com.example.cherryai.Screen

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.cherryai.Navigation.Routes
import com.example.cherryai.notifications.NotificationPreferences
import com.example.cherryai.notifications.NotificationPreferencesViewModel

private val PageBg = Color(0xFFF8F9FB)
private val Navy = Color(0xFF1E1B4B)
private val Muted = Color(0xFF94A3B8)
private val Accent = Color(0xFF534CFF)

@Composable
fun NotificationsScreen(
    navController: NavHostController,
    viewModel: NotificationPreferencesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val snackbarMessage by viewModel.snackbar.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var showSecurityWarning by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* toggled state already optimistic */ }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (showSecurityWarning) {
        AlertDialog(
            onDismissRequest = { showSecurityWarning = false },
            title = { Text("Turn off security alerts?") },
            text = {
                Text(
                    "You'll still see alerts in Security. We recommend keeping this on for sign-ins you don't recognize."
                )
            },
            confirmButton = {
                TextButton(onClick = { showSecurityWarning = false }) { Text("Keep on") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSecurityWarning = false
                        viewModel.setCategory("security_alerts", false)
                    },
                ) { Text("Turn off") }
            },
        )
    }

    val prefs = when (val s = state) {
        is NotificationPreferencesViewModel.UiState.Ready -> s.prefs
        is NotificationPreferencesViewModel.UiState.Error -> s.lastKnown
        else -> null
    }

    val notificationsDenied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = PageBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(40.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { navController.popBackStack() },
                    tint = Navy,
                )
                Spacer(Modifier.width(12.dp))
                Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Navy)
            }
            Text(
                "Choose how we contact you about chats, plans, usage, and security.",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 40.dp, top = 6.dp),
            )
            Spacer(Modifier.height(24.dp))

            when {
                state is NotificationPreferencesViewModel.UiState.Loading -> {
                    NotificationPreferencesPlaceholder()
                }
                prefs != null -> {
                    if (notificationsDenied && prefs.channels.pushEnabled) {
                        NotificationPermissionCard(context)
                        Spacer(Modifier.height(12.dp))
                    }
                    NotifSectionTitle("Master switches")
                    NotifToggleRow(
                        "Push notifications",
                        "Alerts on this device",
                        Icons.Outlined.Notifications,
                        prefs.channels.pushEnabled,
                    ) { enabled ->
                        viewModel.setPushEnabled(enabled) { requestNotificationPermission() }
                    }
                    NotifToggleRow(
                        "Email",
                        if (prefs.serverEmailEnabled) "Optional account emails" else "Email delivery is not enabled on the server yet",
                        Icons.Outlined.Email,
                        prefs.channels.emailEnabled,
                        enabled = prefs.serverEmailEnabled,
                    ) { viewModel.setEmailEnabled(it) }
                    NotifToggleRow(
                        "In-app",
                        "Banners and badges inside Cherry AI",
                        Icons.Outlined.AppSettingsAlt,
                        prefs.channels.inAppEnabled,
                    ) { viewModel.setInAppEnabled(it) }

                    NotifSectionTitle("Chat & responses")
                    NotifToggleRow(
                        "Chat & response updates",
                        "When a response finishes while you're away",
                        Icons.Outlined.Chat,
                        prefs.categories.chatUpdates,
                    ) { viewModel.setCategory("chat_updates", it) }

                    NotifSectionTitle("Plan & subscription")
                    NotifToggleRow(
                        "Plan & subscription updates",
                        "Expiry reminders and plan changes",
                        Icons.Outlined.CardMembership,
                        prefs.categories.planUpdates,
                    ) { viewModel.setCategory("plan_updates", it) }

                    NotifSectionTitle("Usage & limits")
                    NotifToggleRow(
                        "Usage & limits",
                        "Allowance warnings and free-models-only mode",
                        Icons.Outlined.Speed,
                        prefs.categories.usageAlerts,
                    ) { viewModel.setCategory("usage_alerts", it) }

                    NotifSectionTitle("Security")
                    NotifNavRow(
                        "Recent security alerts",
                        prefs.securityAlertsSubtitle(),
                        Icons.Outlined.NotificationsActive,
                        onClick = { navController.navigate(Routes.security_alerts) },
                    )
                    NotifToggleRow(
                        "Security push & email",
                        "New sign-ins, password changes, and sign-outs",
                        Icons.Outlined.Shield,
                        prefs.categories.securityAlerts,
                    ) { enabled ->
                        if (!enabled) showSecurityWarning = true
                        else viewModel.setCategory("security_alerts", true)
                    }

                    NotifSectionTitle("Offline model")
                    NotifToggleRow(
                        "Offline downloads",
                        "Download complete or failed on this device",
                        Icons.Outlined.Download,
                        prefs.categories.offlineDownloads,
                    ) { viewModel.setCategory("offline_downloads", it) }

                    NotifSectionTitle("Offers & tips")
                    NotifToggleRow(
                        "Promotional & offers",
                        "Opt in to offers, upgrades, and new features",
                        Icons.Outlined.LocalOffer,
                        prefs.categories.promotional,
                    ) { viewModel.setCategory("promotional", it) }
                }
                state is NotificationPreferencesViewModel.UiState.Error -> {
                    val errorState = state as NotificationPreferencesViewModel.UiState.Error
                    Text(
                        errorState.message,
                        color = Muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                    Button(onClick = { viewModel.load() }) { Text("Try again") }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NotificationPreferencesPlaceholder() {
    val rows = listOf(
        "Master switches" to 3,
        "Chat & responses" to 1,
        "Plan & subscription" to 1,
        "Usage & limits" to 1,
        "Security" to 2,
        "Offline model" to 1,
        "Offers & tips" to 1,
    )
    rows.forEach { (title, count) ->
        Box(
            Modifier
                .padding(top = 12.dp, bottom = 8.dp)
                .width(130.dp)
                .height(14.dp)
                .background(Color(0xFFE2E8F0), RoundedCornerShape(7.dp)),
        )
        repeat(count) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(bottom = 10.dp)
                    .background(Color(0xFFEFF2F6), RoundedCornerShape(16.dp)),
            )
        }
    }
}

@Composable
private fun NotificationPermissionCard(context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Notifications are blocked", fontWeight = FontWeight.SemiBold, color = Navy)
            Text(
                "Enable notifications in system settings to receive push alerts.",
                fontSize = 13.sp,
                color = Muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Open settings") }
        }
    }
}

@Composable
private fun NotifSectionTitle(text: String) {
    Text(
        text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = Muted,
        modifier = Modifier.padding(bottom = 8.dp, top = 12.dp),
    )
}

@Composable
private fun NotifToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = if (enabled) Accent else Muted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = if (enabled) Navy else Muted)
                Text(subtitle, fontSize = 12.sp, color = Muted, lineHeight = 16.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun NotifNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Navy)
                Text(subtitle, fontSize = 12.sp, color = Muted, lineHeight = 16.sp)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = Color(0xFFC8C8D4))
        }
    }
}
