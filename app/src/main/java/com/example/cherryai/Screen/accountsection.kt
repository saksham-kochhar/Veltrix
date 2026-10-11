package com.example.cherryai.Screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.cherryai.Navigation.Routes
import com.example.cherryai.WalletSnapshot
import com.example.cherryai.cherryAiViewModel

private val AccountBg = Color(0xFFF5F6FA)
private val AccountNavy = Color(0xFF1A1A3A)
private val AccountMuted = Color(0xFF8A8A98)
private val AccountAccent = Color(0xFF5B4DFF)
private val AccountTrack = Color(0xFFE8E8F0)

@Composable
fun AccountScreen(navcontroller: NavHostController, viewmodel: cherryAiViewModel) {
    val profile by viewmodel.userProfile.collectAsState()
    val plan = profile?.plan?.lowercase() ?: "free"
    val wallet = viewmodel.walletSnapshot
    LaunchedEffect(Unit) {
        viewmodel.fetchWallet()
    }
    val fullName = "${profile?.firstname.orEmpty()} ${profile?.lastname.orEmpty()}".trim()

    val menuItems = listOf(
        AccountMenuItem(
            title = "Profile Information",
            subtitle = "View and edit your personal details",
            icon = Icons.Outlined.Person,
            iconBg = Color(0xFFE8F8EF),
            iconTint = Color(0xFF16A34A)
        ),
        AccountMenuItem(
            title = "Security",
            subtitle = "Password, 2FA and account security",
            icon = Icons.Outlined.Security,
            iconBg = Color(0xFFE8F0FF),
            iconTint = Color(0xFF2563EB)
        ),
        AccountMenuItem(
            title = "Notifications",
            subtitle = "Manage your notification preferences",
            icon = Icons.Outlined.Notifications,
            iconBg = Color(0xFFF3E8FF),
            iconTint = Color(0xFF7C4DFF)
        ),
        AccountMenuItem(
            title = "Offline Data",
            subtitle = "Manage downloaded models and data",
            icon = Icons.Outlined.CloudDownload,
            iconBg = Color(0xFFE0F7FA),
            iconTint = Color(0xFF009688)
        ),
        AccountMenuItem(
            title = "Billing & Subscription",
            subtitle = "Manage your payments and plan",
            icon = Icons.Outlined.CreditCard,
            iconBg = Color(0xFFFFF3E0),
            iconTint = Color(0xFFF57C00)
        ),
        AccountMenuItem(
            title = "Help & Support",
            subtitle = "Get help and contact support",
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            iconBg = Color(0xFFFCE8F3),
            iconTint = Color(0xFFE91E8C)
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AccountBg)
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { navcontroller.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1C1C3A),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Account",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = AccountNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage your profile and preferences",
                color = AccountMuted,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileAvatar(
                            firstName = profile?.firstname,
                            lastName = profile?.lastname,
                            size = 48.dp,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fullName.ifEmpty { "User" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = AccountNavy
                            )
                            Text(
                                text = profile?.email ?: "Unable to fetch details",
                                color = AccountMuted,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    PlanStatusBanner(
                        plan = plan,
                        freeOnlyMode = wallet.freeOnlyMode,
                        onViewPlan = { navcontroller.navigate(Routes.details) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F5))
                    Spacer(modifier = Modifier.height(16.dp))

                    UsageAllowanceSection(wallet = wallet)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    menuItems.forEachIndexed { index, item ->
                        AccountMenuRow(item) {
                            when (item.title) {
                                "Profile Information" -> navcontroller.navigate(Routes.profile)
                                "Security" -> navcontroller.navigate(Routes.security)
                                "Notifications" -> navcontroller.navigate(Routes.notifications)
                                "Offline Data" -> navcontroller.navigate(Routes.offlineData)
                                "Billing & Subscription" -> navcontroller.navigate(Routes.details)
                                "Help & Support" -> navcontroller.navigate(Routes.helpSupport)
                            }
                        }
                        if (index != menuItems.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color(0xFFF0F0F5)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewmodel.signout()
                        navcontroller.navigate(Routes.Auth) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Log out",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFFE53935)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun UsageAllowanceSection(wallet: WalletSnapshot) {
    val monthlyPercent = wallet.monthlyUsedPercent
    val dailyPercent = wallet.dailyUsedPercent

    Text(
        text = "Usage",
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = AccountNavy
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        UsagePercentRing(
            label = "This month",
            percent = monthlyPercent,
            accent = AccountAccent,
            modifier = Modifier.weight(1f)
        )
        UsagePercentRing(
            label = "Today",
            percent = dailyPercent,
            accent = Color(0xFF00A651),
            modifier = Modifier.weight(1f)
        )
    }
    Text(
        text = "Monthly remaining: ${wallet.monthlyRemainingPercent}% · Daily remaining: ${wallet.dailyRemainingPercent}%",
        fontSize = 12.sp,
        color = AccountMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
    if (wallet.freeOnlyMode) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Monthly allowance reached — free models only until reset.",
            fontSize = 12.sp,
            color = Color(0xFFB45309),
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun UsagePercentRing(
    label: String,
    percent: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 9.dp.toPx()
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = AccountTrack,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * (percent / 100f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
            Text(
                text = "$percent%",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = AccountNavy
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AccountMuted
        )
    }
}

private fun usagePercent(used: Long, limit: Long): Int {
    if (limit <= 0L) return 0
    return ((used.toDouble() / limit.toDouble()) * 100.0)
        .toInt()
        .coerceIn(0, 100)
}

@Composable
private fun PlanStatusBanner(
    plan: String,
    freeOnlyMode: Boolean = false,
    onViewPlan: () -> Unit = {}
) {
    val (bannerBg, accentColor, title, subtitle, icon) = when (plan) {
        "pro" -> PlanBannerStyle(
            bannerBg = Color(0xFFE9FFF0),
            accentColor = Color(0xFF00A651),
            title = "You're on Pro",
            subtitle = if (freeOnlyMode) "Free models only until credits reset" else "Enjoy all Pro features",
            icon = Icons.Default.WorkspacePremium
        )

        "pro_plus" -> PlanBannerStyle(
            bannerBg = Color(0xFFE8F4FF),
            accentColor = Color(0xFF2563EB),
            title = "You're on Pro Plus",
            subtitle = if (freeOnlyMode) "Free models only until credits reset" else "Premium models included",
            icon = Icons.Default.WorkspacePremium
        )

        "ultra" -> PlanBannerStyle(
            bannerBg = Color(0xFFEAF8EE),
            accentColor = Color(0xFF16A34A),
            title = "You're on Ultra",
            subtitle = if (freeOnlyMode) "Free models only until credits reset" else "Enjoy all Premium features",
            icon = Icons.Default.Diamond
        )

        else -> PlanBannerStyle(
            bannerBg = Color(0xFFF0EEFF),
            accentColor = AccountAccent,
            title = "You're on Free",
            subtitle = "Upgrade for a better experience",
            icon = Icons.Default.Redeem
        )
    }

    Surface(
        color = bannerBg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (plan == "pro") Icons.Default.AutoAwesome else icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = accentColor.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Button(
                onClick = onViewPlan,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                contentPadding = ButtonDefaults.ContentPadding,
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "View Plan",
                    color = Color.White,
                    fontSize = 12.sp,
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
fun AccountMenuRow(item: AccountMenuItem, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(item.iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = item.iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = AccountNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                item.subtitle,
                color = AccountMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFC8C8D4),
            modifier = Modifier.size(22.dp)
        )
    }
}

data class AccountMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBg: Color = Color(0xFFEAF8EE),
    val iconTint: Color = Color(0xFF16A34A)
)
