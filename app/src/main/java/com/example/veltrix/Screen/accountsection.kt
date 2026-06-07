package com.example.veltrix.Screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.veltrix.Navigation.Routes
import com.example.veltrix.R
import com.example.veltrix.veltrixviewmodel

@Composable
fun AccountScreen(navcontroller : NavHostController , viewmodel : veltrixviewmodel) {

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
                .padding(top = 40.dp)
                .background(Color(0xFFF8F8F8)),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        IconButton(
                            onClick = { navcontroller.popBackStack() },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(Icons.Default.ArrowBackIosNew, null)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = "Account",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Text(
                                text = "Manage your profile and preferences",
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            item {

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                )

                                FloatingActionButton(
                                    onClick = {},
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.BottomEnd),
                                    containerColor = Color(0xFF22C55E)
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        null,
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Saksham Kochhar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E))
                                            .clickable { /* Enter Name */ },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "exampleemail123@gmail.com",
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text("Free Mode")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Star,
                                            null
                                        )
                                    }
                                )
                            }

                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                null
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFEAF8EE)
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    Icons.Default.Diamond,
                                    null,
                                    tint = Color(0xFF22C55E)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        "You're on Premium",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )

                                    Text(
                                        "Enjoy all premium features",
                                        color = Color.Gray
                                    )
                                }

                                OutlinedButton(
                                    onClick = {}
                                ) {
                                    Text("View Plan")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            item {

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {

                    menuItems.forEachIndexed { index, item ->

                        AccountMenuRow(item)

                        if (index != menuItems.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            item {

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3F3)
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            null,
                            tint = Color.Red
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(2.dp))
                                .clickable {
                                    viewmodel.signout()
                                    navcontroller.navigate(Routes.Auth) {
                                        popUpTo(0) { inclusive = true }
                                    }


                            }
                        ) {

                            Text(
                                "Log Out",
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )

                            Text(
                                "Sign out from your account",
                                color = Color.Gray
                            )
                        }

                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            null
                        )
                    }
                }
            }
        }
}

@Composable
fun AccountMenuRow(
    item: AccountMenuItem
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    Color(0xFFEAF8EE),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                item.icon,
                null,
                tint = Color(0xFF16A34A)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                item.title,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                item.subtitle,
                color = Color.Gray,
                fontSize = 13.sp
            )
        }

        Icon(
            Icons.Default.KeyboardArrowRight,
            null
        )
    }
}

data class AccountMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)