package com.example.veltrix.Screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Transgender
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.veltrix.veltrixviewmodel
import kotlin.math.abs
import kotlinx.coroutines.launch

private val PageBackground = Color(0xFFF8F9FB)
private val Navy = Color(0xFF1E1B4B)
private val Muted = Color(0xFF94A3B8)
private val Border = Color(0xFFE2E8F0)
private val Accent = Color(0xFF534CFF)
private val Selected = Color(0xFFEDE9FE)
private val FemaleSelected = Color(0xFFFFE8F1)
private val OtherSelected = Color(0xFFE5F7EE)
private val FemaleAccent = Color(0xFFE85D91)
private val OtherAccent = Color(0xFF3AAB73)
private val HeaderIcon = Color(0xFF1C1C3A)
private val DisabledFieldBg = Color(0xFFF1F3F6)
private val DisabledFieldText = Color(0xFF94A3B8)

private const val MinAge = 13
private const val MaxAge = 100
private val AgeRange = (MinAge..MaxAge).toList()

@Composable
fun ProfileInformationScreen(navController: NavHostController, viewModel: veltrixviewmodel) {
    val profile by viewModel.userProfile.collectAsState()
    var name by remember(profile) { mutableStateOf("${profile?.firstname.orEmpty()} ${profile?.lastname.orEmpty()}".trim()) }
    var phone by remember(profile) { mutableStateOf(profile?.phoneNumber.orEmpty().filter(Char::isDigit).takeLast(15)) }
    var gender by remember(profile) { mutableStateOf(profile?.gender.orEmpty().ifBlank { "Male" }) }
    var age by remember(profile) { mutableIntStateOf(profile?.age?.takeIf { it > 0 } ?: 22) }
    val email = profile?.email.orEmpty().ifBlank { viewModel.email }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable { navController.popBackStack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = HeaderIcon,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Profile Information",
            color = Navy,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Update your personal details to personalize your experience.",
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(32.dp))

        ProfileLabel("Full Name")
        ProfileField(name, { name = it }, Icons.Outlined.Person)
        Spacer(Modifier.height(24.dp))

        ProfileLabel("Email Address")
        ProfileEmailField(email)
        Text(
            text = "This email is tied to your account and can't be changed.",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(20.dp))

        ProfileLabel("Phone Number")
        PhoneField(phone) { phone = it.filter(Char::isDigit).take(15) }
        Spacer(Modifier.height(28.dp))

        ProfileLabel("Gender")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GenderOption("Male", Icons.Outlined.Male, gender == "Male", Modifier.weight(1f)) { gender = "Male" }
            GenderOption("Female", Icons.Outlined.Female, gender == "Female", Modifier.weight(1f)) { gender = "Female" }
            GenderOption("Other", Icons.Outlined.Transgender, gender == "Other", Modifier.weight(1f)) { gender = "Other" }
        }
        Spacer(Modifier.height(28.dp))

        ProfileLabel("Age")
        Spacer(Modifier.height(4.dp))
        AgeCarousel(selectedAge = age, onAgeSelected = { age = it })
        Spacer(Modifier.height(40.dp))

        Button(
            onClick = { viewModel.updateProfile(name, phone, gender, age) { navController.popBackStack() } },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgeCarousel(selectedAge: Int, onAgeSelected: (Int) -> Unit) {
    val itemWidth = 52.dp
    val carouselHeight = 56.dp
    val configuration = LocalConfiguration.current
    val sidePadding = (configuration.screenWidthDp.dp - itemWidth - 40.dp) / 2

    val initialIndex = (selectedAge - MinAge).coerceIn(0, AgeRange.lastIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                initialIndex
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                layoutInfo.visibleItemsInfo
                    .minByOrNull { item -> abs(item.offset + item.size / 2 - viewportCenter) }
                    ?.index
                    ?.coerceIn(0, AgeRange.lastIndex)
                    ?: listState.firstVisibleItemIndex.coerceIn(0, AgeRange.lastIndex)
            }
        }
    }

    LaunchedEffect(centeredIndex) {
        val newAge = AgeRange[centeredIndex]
        if (newAge != selectedAge) onAgeSelected(newAge)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(carouselHeight)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to PageBackground,
                        0.12f to Color.Transparent,
                        0.88f to Color.Transparent,
                        1f to PageBackground
                    )
                )
            }
    ) {
        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(horizontal = sidePadding.coerceAtLeast(0.dp)),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            items(AgeRange.size) { index ->
                val value = AgeRange[index]
                val isSelected = index == centeredIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(carouselHeight)
                ) {
                    if (index > 0) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(Border)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp)
                            .background(
                                if (isSelected) Selected else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                scope.launch { listState.animateScrollToItem(index) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            value.toString(),
                            color = if (isSelected) Accent else Muted,
                            fontSize = if (isSelected) 18.sp else 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileLabel(text: String) {
    Text(text, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun ProfileEmailField(email: String) {
    OutlinedTextField(
        value = email,
        onValueChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .heightIn(min = 52.dp),
        enabled = false,
        readOnly = true,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = {
            Icon(Icons.Outlined.Email, null, Modifier.size(22.dp), tint = DisabledFieldText)
        },
        textStyle = LocalTextStyle.current.copy(
            fontSize = 15.sp,
            color = DisabledFieldText,
            fontWeight = FontWeight.Medium
        ),
        colors = TextFieldDefaults.colors(
            disabledContainerColor = DisabledFieldBg,
            disabledTextColor = DisabledFieldText,
            disabledLeadingIconColor = DisabledFieldText,
            disabledIndicatorColor = Border,
            focusedContainerColor = DisabledFieldBg,
            unfocusedContainerColor = DisabledFieldBg,
            focusedIndicatorColor = Border,
            unfocusedIndicatorColor = Border
        )
    )
}

@Composable
private fun ProfileField(value: String, onChange: (String) -> Unit, icon: ImageVector, enabled: Boolean = true) {
    OutlinedTextField(
        value,
        onChange,
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .heightIn(min = 52.dp),
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = { Icon(icon, null, Modifier.size(22.dp), tint = Muted) },
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = Navy, fontWeight = FontWeight.Medium),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            disabledTextColor = Navy,
            disabledLeadingIconColor = Muted,
            unfocusedIndicatorColor = Border,
            focusedIndicatorColor = Accent,
            disabledIndicatorColor = Border
        )
    )
}

@Composable
private fun PhoneField(phone: String, onChange: (String) -> Unit) {
    val displayPhone = remember(phone) {
        phone.chunked(5).joinToString(" ").trim()
    }
    OutlinedTextField(
        value = displayPhone,
        onValueChange = { raw -> onChange(raw.filter(Char::isDigit)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .heightIn(min = 52.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = { Icon(Icons.Outlined.Phone, null, Modifier.size(22.dp), tint = Muted) },
        prefix = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("+91", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(4.dp))
                Text("⌄", color = Muted, fontSize = 16.sp)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(1.dp).height(22.dp).background(Border))
                Spacer(Modifier.width(10.dp))
            }
        },
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = Navy, fontWeight = FontWeight.Medium),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            unfocusedIndicatorColor = Border,
            focusedIndicatorColor = Accent
        )
    )
}

@Composable
private fun GenderOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val selectedBackground = when (label) {
        "Female" -> FemaleSelected
        "Other" -> OtherSelected
        else -> Selected
    }
    val selectedAccent = when (label) {
        "Female" -> FemaleAccent
        "Other" -> OtherAccent
        else -> Accent
    }
    Surface(
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedBackground else Color.White,
        border = if (selected) null else BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = if (selected) selectedAccent else Muted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = if (selected) selectedAccent else Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
