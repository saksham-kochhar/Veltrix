package com.example.veltrix.Screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.veltrix.Navigation.Routes
import com.example.veltrix.veltrixviewmodel

data class Plan(
    val name: String,
    val price: String,
    val description: List<String>,
    val color: Color
)

@Composable
fun PlanSelectionScreen(viewmodel : veltrixviewmodel , navController: NavHostController) {

    val plans = listOf(
        Plan(
            name = "Free",
            price = "₹0",
            description = listOf(
                "Basic Model",
                "Limited Access",
                "Lower limits"
            ),
            color = Color(0xFF22C55E)
        ),
        Plan(
            name = "Pro",
            price = "₹999",
            description = listOf(
                "Better models",
                "10x limits than free",
                "Better image generation"
            ),
            color = Color(0xFF5B4DFF)
        ),
        Plan(
            name = "Ultra",
            price = "₹1499",
            description = listOf(
                "50x more limit than free",
                "Advanced models",
                "Better reasoning & premium support"
            ),
            color = Color(0xFFFF8A00)
        )
    )
    var selectedPlan by remember { mutableStateOf(plans[0]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8FC))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Veltrix",
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF13144A),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Text(
            text = "Create your account",
            fontSize = 18.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = viewmodel.firstname,
            onValueChange = {
                if(it.length <= 15 && it.all { c -> c.isLetter() }){
                    viewmodel.firstname = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            placeholder = {
                Text("First Name")
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null
                )
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = viewmodel.lastname,
            onValueChange = {
                if(it.length<= 15 && it.all { c -> c.isLetter() }) {
                    viewmodel.lastname = it
                }

            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            placeholder = {
                Text("Last Name")
            },
            leadingIcon = {
                Icon(
                    Icons.Default.PersonOutline,
                    contentDescription = null
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Choose your plan",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Start free. Upgrade anytime.",
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        plans.forEach { plan ->

            val isSelected = selectedPlan == plan

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        selectedPlan = plan
                    },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected)
                        plan.color
                    else
                        Color.LightGray
                ),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = plan.name,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = plan.color,

                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            plan.description.forEach {
                                Text(
                                    text = "✓ $it",
                                    color = Color.DarkGray,
                                    fontSize = 15.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {

                            if (isSelected) {
                                Surface(
                                    color = plan.color.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "Selected",
                                        color = plan.color, maxLines = 1,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Text(
                                text = plan.price,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = plan.color,
                                maxLines = 1
                            )

                            Text(
                                text = "Inclusive taxes",
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewmodel.completeOnboarding(selectedPlan.name.lowercase()) {
                    navController.navigate(Routes.Home) {
                        popUpTo(0) { inclusive = true}
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5A4CFF) ,
                disabledContainerColor = Color(0xFF555555) ,
                disabledContentColor = Color.Gray
            ),
            shape = RoundedCornerShape(30.dp),
            enabled = !viewmodel.firstname.isBlank() || !viewmodel.lastname.isBlank()
        ) {
            Text(
                text = "Continue",
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}