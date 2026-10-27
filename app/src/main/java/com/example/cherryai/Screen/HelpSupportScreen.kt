package com.example.cherryai.Screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.cherryai.cherryAiViewModel
import com.example.cherryai.Navigation.Routes

private const val SUPPORT_EMAIL = "support@example.com"

@Composable
fun HelpSupportScreen(navController: NavHostController, viewModel: cherryAiViewModel) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deleteInProgress by remember { mutableStateOf(false) }
    val faqs = listOf(
        "How do I change my plan?" to "Open Billing & Subscription from Account, then swipe through the available plans and choose one.",
        "Can I use Cherry AI offline?" to "Yes. Download the offline model from the home screen. You can manage downloaded models under Account > Offline Data.",
        "How do I manage notifications?" to "Open Account > Notifications to control push alerts, email alerts, and notification categories.",
        "Where are my chats saved?" to "Chats are saved locally and synchronized when you are signed in and online.",
        "How quickly will support reply?" to "Email support is monitored 24/7. Response times may vary depending on the request."
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F6FA)).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(28.dp))
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1C1C3A))
            }
            Spacer(Modifier.height(10.dp))
            Text("Help & Support", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A3A))
            Text("Find answers or contact our support team.", color = Color(0xFF8A8A98), modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(16.dp))
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    val body = "Hello Cherry AI Support,\n\nPlease describe your issue here.\n\nAccount email: ${profile?.email.orEmpty()}\nApp: Cherry AI Android"
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$SUPPORT_EMAIL")
                        putExtra(Intent.EXTRA_SUBJECT, "Cherry AI support request")
                        putExtra(Intent.EXTRA_TEXT, body)
                    }
                    runCatching { context.startActivity(intent) }.onFailure {
                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9FF))
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Email, contentDescription = null, tint = Color(0xFF5B4DFF))
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text("Email support", fontWeight = FontWeight.Bold, color = Color(0xFF1A1A3A))
                        Text(SUPPORT_EMAIL, color = Color(0xFF5B4DFF), modifier = Modifier.padding(top = 3.dp))
                        Text("Available 24/7", color = Color(0xFF8A8A98), modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Frequently asked questions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A3A))
        }
        items(faqs.size) { index ->
            val (question, answer) = faqs[index]
            var expanded by remember { mutableStateOf(false) }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(question, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A3A))
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color(0xFF8A8A98))
                    }
                    if (expanded) Text(answer, color = Color(0xFF8A8A98), modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cherryai.app/privacy-policy"))
                    runCatching { context.startActivity(intent) }
                },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Privacy policy", fontWeight = FontWeight.Bold, color = Color(0xFF1A1A3A))
                    Text("Read how Cherry AI collects, uses, and deletes your data.", color = Color(0xFF8A8A98), modifier = Modifier.padding(top = 6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cherryai.app/terms-of-service"))
                    runCatching { context.startActivity(intent) }
                },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Terms & Conditions", fontWeight = FontWeight.Bold, color = Color(0xFF1A1A3A))
                    Text("Review the rules and conditions for using Cherry AI.", color = Color(0xFF8A8A98), modifier = Modifier.padding(top = 6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showDeleteConfirmation = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F1))
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Delete account", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    Text(
                        "Hide your profile for 30 days before permanent deletion. Your chats are retained securely during this period.",
                        color = Color(0xFF8A8A98),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!deleteInProgress) showDeleteConfirmation = false },
            title = { Text("Delete account?") },
            text = { Text("Your profile will be hidden and you will be signed out. You have 30 days to recover your account; after that, your profile, chats, tokens, sessions, wallet data, and Firebase account will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    enabled = !deleteInProgress,
                    onClick = {
                        deleteInProgress = true
                        viewModel.deleteAccount { result ->
                            result.onSuccess {
                                showDeleteConfirmation = false
                                navController.navigate(Routes.Auth) { popUpTo(0) { inclusive = true } }
                            }.onFailure {
                                deleteInProgress = false
                                Toast.makeText(context, "Couldn't delete account. Please try again.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text(if (deleteInProgress) "Deleting…" else "Delete account", color = Color(0xFFC62828)) }
            },
            dismissButton = { TextButton(enabled = !deleteInProgress, onClick = { showDeleteConfirmation = false }) { Text("Cancel") } }
        )
    }
}
