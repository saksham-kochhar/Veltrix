package com.example.veltrix.Screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.veltrix.Authstate
import com.example.veltrix.Navigation.Routes
import com.example.veltrix.R
import com.example.veltrix.veltrixviewmodel

fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

@Composable
fun AuthScreen(
    onGoogleClick: () -> Unit = {},
    onForgotPassword: (email: String) -> Unit = {},
    viewModel: veltrixviewmodel,
    navController: NavHostController
) {
    var selectedTab by remember { mutableStateOf(0) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val authstate by viewModel.authstate.observeAsState(Authstate.Unauthenticated)
    val primaryBlue = Color(0xFF5A4CFF)

    val indicatorOffset by animateFloatAsState(
        targetValue = if (selectedTab == 0) 0f else 0.5f,
        label = "tabIndicator"
    )

    val isLoading = authstate is Authstate.Loading

    LaunchedEffect(authstate) {
        when (authstate) {
            is Authstate.Authenticated -> navController.navigate(Routes.Home) {
                popUpTo(0) { inclusive = true }
            }
            is Authstate.VerificationSent -> navController.navigate(Routes.verification) {
                popUpTo(Routes.Auth) { inclusive = false }
            }
            else -> Unit
        }
    }

    val displayError = when {
        errorMessage.isNotEmpty() -> errorMessage
        authstate is Authstate.Error -> (authstate as Authstate.Error).message
        else -> ""
    }

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

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Log In", "Sign Up").forEachIndexed { index, title ->
                    Text(
                        text = title,
                        color = if (selectedTab == index) primaryBlue else Color.Gray,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = index
                                viewModel.resetState()}
                            .padding(bottom = 10.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }

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
                        .offset(x = (indicatorOffset * 400).dp)
                        .background(primaryBlue)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = viewModel.email,
                onValueChange = {
                    viewModel.email = it
                    viewModel.resetState()
                },
                leadingIcon = { Icon(Icons.Outlined.Email, null) },
                trailingIcon = {
                    if (viewModel.email.isNotEmpty()) {
                        if (android.util.Patterns.EMAIL_ADDRESS.matcher(viewModel.email).matches()) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.Green)
                        } else {
                            Icon(Icons.Outlined.Close, contentDescription = null, tint = Color.Red)
                        }
                    }
                },
                placeholder = { Text("Email address") },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = displayError.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.resetState()
                },
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

            if (selectedTab == 1) {
                Spacer(modifier = Modifier.height(18.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        viewModel.resetState()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Outlined.Visibility
                                else Icons.Outlined.VisibilityOff,
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

            if (selectedTab == 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = "Forgot password?",
                        color = primaryBlue,
                        modifier = Modifier.clickable { onForgotPassword(viewModel.email) }
                    )
                }
            }

            if (displayError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = displayError, color = Color.Red, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    errorMessage = ""
                    when {
                        viewModel.email.isBlank() || password.isBlank() ->
                            errorMessage = "Please fill in all fields"
                        !isValidEmail(viewModel.email) ->
                            errorMessage = "Enter a valid email address"
                        selectedTab == 1 && password != confirmPassword ->
                            errorMessage = "Passwords do not match"
                        selectedTab == 0 -> viewModel.login(viewModel.email, password)
                        else -> viewModel.signup(viewModel.email, password)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier.fillMaxWidth().height(62.dp),
                enabled = password.length in 8..20 && !isLoading
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
                    if (isLoading) {
                        PulseDots()
                    } else {
                        Text(
                            text = if (selectedTab == 0) "Log In" else "Sign Up",
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(35.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(text = "  or  ", color = Color.Gray)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(35.dp))

            OutlinedButton(
                onClick = onGoogleClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Icon",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Continue with Google", fontSize = 14.sp, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
@Composable
fun PulseDots() {
    val dotCount = 3
    val delays = listOf(0, 200, 400)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        delays.forEach { delay ->
            val infiniteTransition = rememberInfiniteTransition(label = "dot")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0.6f at 0
                        1f at 400
                        0.6f at 800
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(delay)
                ),
                label = "scale"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0.3f at 0
                        1f at 400
                        0.3f at 800
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(delay)
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}