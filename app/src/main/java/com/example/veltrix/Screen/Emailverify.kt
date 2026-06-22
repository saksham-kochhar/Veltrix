package com.example.veltrix.Screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.veltrix.Authstate
import com.example.veltrix.Navigation.Routes
import com.example.veltrix.veltrixviewmodel

@Composable
fun VerificationScreen(viewModel: veltrixviewmodel, navController: NavHostController)
{
    val authstate by viewModel.authstate.observeAsState(Authstate.Unauthenticated)
    LaunchedEffect(Unit) {
        viewModel.startVerificationPolling()
    }
    LaunchedEffect(authstate) {
        when (authstate) {
            is Authstate.Authenticated -> navController.navigate(Routes.Home) {
                popUpTo(0) { inclusive = true }
            }
            is Authstate.ProfileIncomplete -> navController.navigate(Routes.details) {
                popUpTo(0) { inclusive = true }
            }
            else -> {}
        }
    }
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(50.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = {navController.popBackStack()}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0A0A2A)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))


        Text(
            text = "Check your email",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0A0A2A)
        )

        Spacer(modifier = Modifier.height(40.dp))


        Icon(
            imageVector = Icons.Default.MarkEmailRead,
            contentDescription = null,
            tint = Color(0xFF5B4BFF),
            modifier = Modifier.size(140.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "We've sent a verification link to",
            color = Color.Gray,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = viewModel.email,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Please check your inbox and click the link\nto verify your account.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 18.sp,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { viewModel.openEmailApp(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5B4BFF)
            )
        ) {
            Text(
                text = "Open Email App",
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color(0xFFE5E5E5)
            )

            Text(
                text = "  or  ",
                color = Color.Gray,
                fontSize = 16.sp
            )

            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color(0xFFE5E5E5)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { viewModel.resendVerificationEmail() },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                1.dp,
                Color(0xFFE0E0E0)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = Color(0xFF5B4BFF)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Resend verification email",
                color = Color(0xFF5B4BFF),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Didn't receive the email? Check your spam folder.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}