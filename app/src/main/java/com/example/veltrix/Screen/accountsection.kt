package com.example.veltrix.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.veltrix.Navigation.Routes
import com.example.veltrix.veltrixviewmodel

@Composable
fun AccountScreen(navcontroller: NavHostController, viewmodel: veltrixviewmodel) {
    val profile by viewmodel.userProfile.collectAsState()
    val plan = profile?.plan?.lowercase() ?: "free"
    val fullName = "${profile?.firstname.orEmpty()} ${profile?.lastname.orEmpty()}".trim()

    val menuItems = listOf(
        AccountMenuItem(
            title = "Profile Information",
            subtitle = "View and edit your personal details",
            icon = Icons.Outlined.Person
        ),
        AccountMenuItem(
            title = "Security",
            subtitle = "Password, 2FA and account security",
            icon = Icons.Outlined.Security
        ),
        AccountMenuItem(
            title = "Notifications",
            subtitle = "Manage your notification preferences",
            icon = Icons.Outlined.Notifications
        ),
        AccountMenuItem(
            title = "Offline Data",
            subtitle = "Manage downloaded models and data",
            icon = Icons.Outlined.CloudDownload
        ),
        AccountMenuItem(
            title = "Billing & Subscription",
            subtitle = "Manage your payments and plan",
            icon = Icons.Outlined.CreditCard
        ),
        AccountMenuItem(
            title = "Help & Support",
            subtitle = "Get help and contact support",
            icon = Icons.AutoMirrored.Outlined.HelpOutline
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    onClick = { navcontroller.popBackStack() },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF555555)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE9FFF0))
                    )
                    ProfileAvatar(
                        firstName = profile?.firstname,
                        lastName = profile?.lastname,
                        size = 108.dp,
                        fontSize = 38.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color(0xFF111133)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Manage your profile and preferences",
                color = Color.Gray,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fullName.ifEmpty { "User" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF111133)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = profile?.email ?: "Unable to fetch details",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFFBBBBBB)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PlanStatusBanner(plan = plan)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        item {
            Text(
                text = "Account",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF111133)
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        items(menuItems.size) { index ->
            AccountMenuRow(menuItems[index])
            if (index != menuItems.lastIndex) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewmodel.signout()
                            navcontroller.navigate(Routes.Auth) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFFFFE5E5), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = Color(0xFFE53935)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Log Out",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                        Text(
                            "Sign out from your account",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }

                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFBBBBBB)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PlanStatusBanner(plan: String) {
    val (bannerBg, accentColor, title, subtitle, icon) = when (plan) {
        "pro" -> PlanBannerStyle(
            bannerBg = Color(0xFFE9FFF0),
            accentColor = Color(0xFF00A651),
            title = "You're on Pro",
            subtitle = "Enjoy all Pro features",
            icon = Icons.Default.WorkspacePremium
        )

        "ultra" -> PlanBannerStyle(
            bannerBg = Color(0xFFEAF8EE),
            accentColor = Color(0xFF16A34A),
            title = "You're on Ultra",
            subtitle = "Enjoy all Premium features",
            icon = Icons.Default.Diamond
        )

        else -> PlanBannerStyle(
            bannerBg = Color(0xFFF3E8FF),
            accentColor = Color(0xFF8543C0),
            title = "You're on Free",
            subtitle = "Upgrade for a better experience",
            icon = Icons.Default.Redeem
        )
    }

    Surface(
        color = bannerBg,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (plan == "pro") Icons.Default.AutoAwesome else icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    color = accentColor.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = {},
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text(
                    text = "View Plan",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private data class PlanBannerStyle(
    val bannerBg: Color,
    val accentColor: Color,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun AccountMenuRow(item: AccountMenuItem) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFFEAF8EE), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = Color(0xFF16A34A))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111133)
                )
                Text(item.subtitle, color = Color.Gray, fontSize = 13.sp)
            }

            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFBBBBBB)
            )
        }
    }
}

data class AccountMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)
