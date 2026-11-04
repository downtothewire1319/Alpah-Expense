package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LightNavy
import com.example.ui.theme.NavyCard
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftWhite

@Composable
fun PinKeypad(
    title: String = "Enter Security PIN",
    subtitle: String = "Security verification passcode",
    maxPinLength: Int = 4,
    errorMessage: String? = null,
    onPinComplete: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lock icon badge
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = AccentGold.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = AccentGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SoftWhite.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Forgot PIN? Reset Passcode",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPinComplete("RESET_PIN_TRIGGER") }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("forgot_pin_button")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until maxPinLength) {
                    val isFilled = i < pin.length
                    val dotColor by animateColorAsState(
                        targetValue = if (isFilled) AccentCyan else LightNavy,
                        animationSpec = tween(durationMillis = 200),
                        label = "dot_color"
                    )

                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(
                                width = 1.5.dp,
                                color = if (isFilled) AccentCyan else SoftWhite.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Numeric Keypad Grid
            val numberRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                numberRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            KeypadButton(
                                label = key,
                                onClick = {
                                    when (key) {
                                        "C" -> pin = ""
                                        "DEL" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        else -> {
                                            if (pin.length < maxPinLength) {
                                                val newPin = pin + key
                                                pin = newPin
                                                if (newPin.length == maxPinLength) {
                                                    onPinComplete(newPin)
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("pin_key_$label"),
        shape = CircleShape,
        color = if (label == "C" || label == "DEL") DeepNavy else LightNavy
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (label) {
                "C" -> Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = SoftWhite.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
                "DEL" -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = SoftWhite.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
                else -> Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite,
                        fontSize = 24.sp
                    )
                )
            }
        }
    }
}
