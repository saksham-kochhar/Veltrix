package com.example.veltrix.Screen

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.veltrix.Navigation.Routes
import com.example.veltrix.R
import com.example.veltrix.veltrixviewmodel



@Composable
fun PlanSelectionScreen(viewmodel: veltrixviewmodel, navController: NavHostController) {

    val primaryBlue = Color(0xFF5A4CFF)
    // Names pre-filled by Google at composition time → lock them
    val namesFromGoogle = remember { viewmodel.firstname.isNotBlank() && viewmodel.lastname.isNotBlank() }
    val fieldColors = TextFieldDefaults.colors(
        focusedTextColor = Color(0xFF1A1A2E),
        unfocusedTextColor = Color(0xFF1A1A2E),
        disabledTextColor = Color(0xFF444466),
        focusedPlaceholderColor = Color(0xFF9E9E9E),
        unfocusedPlaceholderColor = Color(0xFF9E9E9E),
        focusedIndicatorColor = Color(0xFF5C6BC0),
        unfocusedIndicatorColor = Color(0xFFBDBDBD),
        disabledIndicatorColor = Color(0xFFBDBDBD),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8FC))
            .verticalScroll(rememberScrollState())
    ) {

        // ── Header ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 52.dp, bottom = 28.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val ctx = LocalContext.current
            val bitmap = remember {
                val drawable = ctx.getDrawable(R.mipmap.ic_launcher)!!
                val bmp = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
                drawable.setBounds(0, 0, 192, 192)
                drawable.draw(Canvas(bmp))
                bmp.asImageBitmap()
            }
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(92.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Let's get to know you",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF13144A),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Enter your details to create your account",
                color = Color.Gray,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }

        // ── Fields ────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {

            // First Name + Last Name side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = viewmodel.firstname,
                    onValueChange = {
                        if (!namesFromGoogle && it.length <= 15 && it.all(Char::isLetter))
                            viewmodel.firstname = it
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    placeholder = { Text("First name") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = primaryBlue) },
                    colors = fieldColors,
                    singleLine = true,
                    readOnly = namesFromGoogle
                )
                OutlinedTextField(
                    value = viewmodel.lastname,
                    onValueChange = {
                        if (!namesFromGoogle && it.length <= 15 && it.all(Char::isLetter))
                            viewmodel.lastname = it
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    placeholder = { Text("Last name") },
                    leadingIcon = { Icon(Icons.Default.PersonOutline, null, tint = primaryBlue) },
                    colors = fieldColors,
                    singleLine = true,
                    readOnly = namesFromGoogle
                )
            }

            Spacer(Modifier.height(14.dp))

            // Email – pre-filled from auth, always read-only
            OutlinedTextField(
                value = viewmodel.email,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                placeholder = { Text("Email address") },
                leadingIcon = { Icon(Icons.Outlined.Email, null, tint = Color(0xFFBDBDBD)) },
                colors = TextFieldDefaults.colors(
                    disabledTextColor = Color(0xFF9E9E9E),
                    disabledLeadingIconColor = Color(0xFFBDBDBD),
                    disabledIndicatorColor = Color(0xFFE0E0E0),
                    disabledContainerColor = Color(0xFFF5F5F5),
                    disabledPlaceholderColor = Color(0xFFBDBDBD),
                ),
                singleLine = true,
                enabled = false,
                supportingText = {
                    Text("Email cannot be changed", color = Color(0xFFBDBDBD), fontSize = 12.sp)
                }
            )

            Spacer(Modifier.height(14.dp))

            // Privacy badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = primaryBlue.copy(alpha = 0.07f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = primaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "We'll never share your information with anyone.",
                        color = Color(0xFF555577),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            // ── Continue Button ───────────────────────────────
            Button(
                onClick = {
                    viewmodel.completeOnboarding("free") {
                        navController.navigate(Routes.Home) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryBlue,
                    disabledContainerColor = Color(0xFF9E99CC)
                ),
                enabled = viewmodel.firstname.isNotBlank() && viewmodel.lastname.isNotBlank()
            ) {
                Text(
                    text = "Continue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
