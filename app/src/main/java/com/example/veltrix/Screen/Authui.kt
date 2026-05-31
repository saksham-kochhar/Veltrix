package com.example.veltrix.Screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(
    onLoginClick: (email: String, password: String) -> Unit = { _, _ -> },
    onSignUpClick: (email: String, password: String) -> Unit = { _, _ -> },
    onGoogleClick: () -> Unit = {},
    onForgotPassword: (email: String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val primaryBlue = Color(0xFF5A4CFF)

    // ✅ Smooth tab indicator animation
    val indicatorOffset by animateFloatAsState(
        targetValue = if (selectedTab == 0) 0f else 0.5f,
        label = "tabIndicator"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8FC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(45.dp))

            Text("Veltrix", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14143A))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Connected or Not", color = Color.Gray, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(40.dp))

            // ✅ Tabs
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Log In", "Sign Up").forEachIndexed { index, title ->
                    Text(
                        text = title,
                        color = if (selectedTab == index) primaryBlue else Color.Gray,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
                            .padding(bottom = 10.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }

            // ✅ Animated tab underline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(2.dp)
                        .offset(x = (indicatorOffset * 400).dp) // Approximate offset
                        .background(primaryBlue)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = "" },
                leadingIcon = { Icon(Icons.Outlined.Email, null) },
                placeholder = { Text("Email address") },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(64.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = "" },
                leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                placeholder = { Text("Password") },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(64.dp),
                singleLine = true
            )

            // ✅ Confirm Password — only on Sign Up tab
            if (selectedTab == 1) {
                Spacer(modifier = Modifier.height(18.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = "" },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                null
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    placeholder = { Text("Confirm Password") },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    singleLine = true
                )
            }

            // ✅ Forgot password only on Log In tab
            if (selectedTab == 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = "Forgot password?",
                        color = primaryBlue,
                        modifier = Modifier.clickable { onForgotPassword(email) }
                    )
                }
            }

            // ✅ Error message
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage, color = Color.Red, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // ✅ Button label changes with tab
            Button(
                onClick = {
                    when {
                        email.isBlank() || password.isBlank() -> errorMessage = "Please fill in all fields"
                        selectedTab == 1 && password != confirmPassword -> errorMessage = "Passwords do not match"
                        selectedTab == 0 -> onLoginClick(email, password)
                        else -> onSignUpClick(email, password)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier.fillMaxWidth().height(62.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF5A4CFF), Color(0xFF6D57FF))),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // ✅ Button text matches tab
                    Text(
                        text = if (selectedTab == 0) "Log In" else "Sign Up",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(35.dp))

            // ✅ Single HorizontalDivider (removed deprecated Divider)
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(text = "  or  ", color = Color.Gray)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(35.dp))

            // Google Button
            OutlinedButton(
                onClick = onGoogleClick,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(62.dp)
            ) {
                Text("G", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continue with Google", color = Color.Black, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            // ✅ Bottom text switches tab correctly
            Row {
                Text(
                    text = if (selectedTab == 0) "Don't have an account? " else "Already have an account? ",
                    color = Color.Gray
                )
                Text(
                    text = if (selectedTab == 0) "Sign Up" else "Log In",
                    color = primaryBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { selectedTab = if (selectedTab == 0) 1 else 0 }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}