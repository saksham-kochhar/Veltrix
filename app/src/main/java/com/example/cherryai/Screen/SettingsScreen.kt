package com.example.cherryai.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.cherryai.ui.theme.ThemeMode
import com.example.cherryai.ui.theme.ThemePreferences
import com.example.cherryai.ui.theme.cherryUiColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
) {
    val colors = cherryUiColors()
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.pageBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { navController.popBackStack() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.iconPrimary,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Settings",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Appearance and app preferences",
            color = colors.textMuted,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(24.dp))

        Text(
            text = "Appearance",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Theme",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Choose light, dark, or match your device",
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = themeMode.label,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            disabledTextColor = colors.textPrimary,
                            focusedBorderColor = colors.composerBorder,
                            unfocusedBorderColor = colors.divider,
                            focusedContainerColor = colors.surfaceMuted,
                            unfocusedContainerColor = colors.surfaceMuted,
                            cursorColor = colors.accent,
                        ),
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = colors.card,
                    ) {
                        ThemeMode.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.label,
                                        color = colors.textPrimary,
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    ThemePreferences.setMode(context, option)
                                    onThemeModeChanged(option)
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
