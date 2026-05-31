package com.example.veltrix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.veltrix.Navigation.navigation
import com.example.veltrix.Screen.ChatbotScreen
import com.example.veltrix.ui.theme.VeltrixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val viewmodel = ViewModelProvider(this)[veltrixviewmodel :: class.java]
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeltrixTheme {
                navigation(viewmodel)
            }
        }
    }
}

