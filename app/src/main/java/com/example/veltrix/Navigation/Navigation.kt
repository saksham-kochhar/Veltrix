package com.example.veltrix.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.veltrix.Screen.AuthScreen
import com.example.veltrix.Screen.ChatbotScreen
import com.example.veltrix.veltrixviewmodel

@Composable
fun navigation(viewModel: veltrixviewmodel){
    val navController = rememberNavController()
    NavHost(navController , startDestination = Routes.Auth){

        composable(Routes.Auth) {
            AuthScreen()
        }
        composable(Routes.Home) {
            ChatbotScreen(viewmodel = viewModel )
        }
    }
}