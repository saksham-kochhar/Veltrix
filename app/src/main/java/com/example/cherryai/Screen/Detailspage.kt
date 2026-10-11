package com.example.cherryai.Screen

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Star
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
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
import com.example.cherryai.Navigation.Routes
import com.example.cherryai.R
import com.example.cherryai.cherryAiViewModel



@Composable
fun LegacyPlanSelectionScreen(viewmodel: cherryAiViewModel, navController: NavHostController) {

    val primaryBlue = Color(0xFF5A4CFF)
    // Names pre-filled by Google at composition time → lock them
    val namesFromGoogle = remember { viewmodel.firstname.isNotBlank() && viewmodel.lastname.isNotBlank() }
    var selectedPlan = remember { androidx.compose.runtime.mutableStateOf("free") }
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

            Text("Choose your plan", fontWeight = FontWeight.SemiBold, color = Color(0xFF13144A))
            listOf(
                "free" to "Free",
                "pro" to "Pro",
                "pro_plus" to "Pro Plus",
                "plus" to "Plus",
                "ultra" to "Ultra"
            ).forEach { (id, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedPlan.value == id,
                        onClick = { selectedPlan.value = id }
                    )
                    Text(label, color = Color(0xFF33334D))
                }
            }

            // ── Continue Button ───────────────────────────────
            Button(
                onClick = {
                    viewmodel.completeOnboarding(selectedPlan.value) {
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

private data class PlanCard(
    val id: String,
    val name: String,
    val price: String,
    val subtitle: String,
    val description: String,
    val features: List<Pair<String, String>>,
    val accent: Color,
    val surface: Color,
    val popular: Boolean = false
)

private val planCards = listOf(
    PlanCard(
        "free", "Free", "₹0", "A simple way to get started",
        "Explore Cherry AI with essential AI access.",
        listOf(
            "75,000 tokens" to "A monthly allowance for everyday chats.",
            "Free models" to "Access the free model collection.",
            "Great for getting started" to "Learn, brainstorm and explore."
        ), Color(0xFF3478F6), Color(0xFFF4F8FF)
    ),
    PlanCard(
        "pro", "Pro", "₹299", "More room for focused work",
        "A balanced plan for regular AI use.",
        listOf(
            "More conversations" to "A larger allowance than Free.",
            "Popular models" to "Access a curated set of capable models.",
            "Built for productivity" to "Study, code and work with confidence."
        ), Color(0xFF5B4DFF), Color(0xFFF7F5FF)
    ),
    PlanCard(
        "pro_plus", "Pro Plus", "₹499", "More power for productivity",
        "Longer conversations with more advanced models.",
        listOf(
            "Higher allowance than Pro" to "Keep complex conversations going.",
            "More advanced models" to "Unlock the next level of the catalog.",
            "Ideal for serious work" to "Coding, research and daily projects."
        ), Color(0xFF5B4DFF), Color(0xFFF7F3FF), popular = true
    ),
    PlanCard(
        "plus", "Plus", "₹799", "For ambitious ideas",
        "More access for creators and demanding workflows.",
        listOf(
            "A larger monthly allowance" to "Use more capable models for longer.",
            "Expanded model access" to "Explore more of the Cherry AI catalog.",
            "Made for heavy usage" to "Handle deeper tasks and longer sessions."
        ), Color(0xFF00A879), Color(0xFFF1FCF8)
    ),
    PlanCard(
        "ultra", "Ultra", "₹999", "The complete Cherry AI experience",
        "Maximum access for your most demanding work.",
        listOf(
            "Largest monthly allowance" to "Stay productive across long sessions.",
            "All available models" to "Unlock the complete model catalog.",
            "For power users" to "Take on advanced work without compromise."
        ), Color(0xFFB46A00), Color(0xFFFFF9ED)
    )
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlanSelectionScreen(viewmodel: cherryAiViewModel, navController: NavHostController) {
    val profile by viewmodel.userProfile.collectAsState()
    val currentPlanId = profile?.plan?.lowercase()?.replace('-', '_')?.ifBlank { "free" } ?: "free"
    val initialIndex = planCards.indexOfFirst { it.id == currentPlanId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { item -> kotlin.math.abs(item.offset + item.size / 2 - viewportCenter) }
                ?.index
                ?.coerceIn(0, planCards.lastIndex)
                ?: listState.firstVisibleItemIndex.coerceIn(0, planCards.lastIndex)
        }
    }
    val selectedPlan = planCards[selectedIndex]
    val currentPlan = planCards.firstOrNull { it.id == currentPlanId } ?: planCards.first()
    val expiry = profile?.planExpiresAt?.toDate()?.let {
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(it)
    } ?: "No expiry date"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFF))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 22.dp, bottom = 28.dp)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(start = 12.dp, top = 2.dp).size(44.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF1C1C3A),
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = "Billing & Subscription",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF13144A),
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 10.dp)
        )
        Text(
            text = "Manage your current plan or explore more access.",
            fontSize = 17.sp,
            lineHeight = 25.sp,
            color = Color(0xFF85859B),
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 8.dp)
        )
        Spacer(Modifier.height(26.dp))

        Text("Explore plans", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF13144A), modifier = Modifier.padding(start = 28.dp, top = 10.dp, bottom = 12.dp))

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(planCards) { index, plan ->
                val distance = kotlin.math.abs(index - selectedIndex).toFloat()
                val scale by animateFloatAsState(
                    targetValue = if (distance == 0f) 1f else 0.93f,
                    animationSpec = tween(280),
                    label = "planScale"
                )
                PlanCardView(
                    plan = plan,
                    scale = scale,
                    selected = index == selectedIndex,
                    isCurrentPlan = plan.id == currentPlan.id,
                    expiry = if (plan.id == currentPlan.id) expiry else null,
                    onClick = { /* snapping keeps selection centered */ }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                viewmodel.completeOnboarding(selectedPlan.id) {
                    navController.navigate(Routes.Home) { popUpTo(0) { inclusive = true } }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .height(58.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = selectedPlan.accent)
        ) {
            if (selectedPlan.id == currentPlan.id) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Current Plan", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Expires: $expiry", fontSize = 12.sp, fontWeight = FontWeight.Normal)
                }
            } else {
                Text("Choose ${selectedPlan.name}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            text = "You can change your plan later.",
            color = Color(0xFF9A9AAF),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp)
        )
    }
}

@Composable
private fun PlanCardView(
    plan: PlanCard,
    scale: Float,
    selected: Boolean,
    isCurrentPlan: Boolean,
    expiry: String?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(326.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(30.dp),
        color = plan.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, plan.accent.copy(alpha = if (selected) 0.9f else 0.18f)),
        shadowElevation = if (selected) 16.dp else 5.dp
    ) {
        Column(Modifier.padding(25.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = plan.accent.copy(alpha = 0.12f), shape = RoundedCornerShape(18.dp)) {
                    Icon(
                        imageVector = when (plan.id) {
                            "ultra" -> Icons.Outlined.Bolt
                            "free" -> Icons.Outlined.AutoAwesome
                            else -> Icons.Outlined.Star
                        },
                        contentDescription = null,
                        tint = plan.accent,
                        modifier = Modifier.padding(16.dp).size(28.dp)
                    )
                }
                if (plan.popular) {
                    Surface(color = plan.accent.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                        Text("Most Popular", color = plan.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                    }
                }
            }
            // Keep the card footprint stable; current-plan details are shown in the action button.
            if (isCurrentPlan) Spacer(Modifier.height(40.dp))
            Spacer(Modifier.height(22.dp))
            Text(plan.name, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF12133E))
            Text(plan.subtitle, fontSize = 16.sp, color = Color(0xFF77778E), modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(plan.price, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF12133E))
                Text(" / month", fontSize = 15.sp, color = Color(0xFF88889C), modifier = Modifier.padding(bottom = 6.dp, start = 5.dp))
            }
            Text(plan.description, fontSize = 14.sp, color = Color(0xFF87879A), modifier = Modifier.padding(top = 7.dp))
            Spacer(Modifier.height(18.dp))
            androidx.compose.material3.Divider(color = plan.accent.copy(alpha = 0.14f))
            Spacer(Modifier.height(14.dp))
            plan.features.forEach { (title, detail) ->
                Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
                    Surface(color = plan.accent.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                        Icon(Icons.Outlined.Check, null, tint = plan.accent, modifier = Modifier.padding(6.dp).size(16.dp))
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF161744))
                        Text(detail, fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFF85859B), modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}
