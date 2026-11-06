package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentRed
import com.example.ui.theme.NavyCard
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftWhite

@Composable
fun PinKeypad(
    title: String = "Security Check",
    subtitle: String = "Enter 4-digit passcode",
    maxPinLength: Int = 4,
    errorMessage: String? = null,
    onPinComplete: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Lock icon
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = AccentGold.copy(alpha = 0.16f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = AccentGold,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Security Check heading
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    fontSize = 20.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Enter 4-digit passcode subtitle
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SoftWhite.copy(alpha = 0.75f),
                    fontSize = 13.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Forgot PIN? Reset Passcode link
            Text(
                text = "Forgot PIN? Reset Passcode",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = AccentCyan,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPinComplete("RESET_PIN_TRIGGER") }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("forgot_pin_button")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Four PIN indicator circles
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until maxPinLength) {
                    val isFilled = i < pin.length
                    val dotColor by animateColorAsState(
                        targetValue = if (isFilled) AccentCyan else Color(0xFF1E293B),
                        animationSpec = tween(durationMillis = 180),
                        label = "dot_color"
                    )

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(
                                width = 1.5.dp,
                                color = if (isFilled) AccentCyan else SoftWhite.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Responsive Numeric Keypad Grid (3 columns, 4 rows)
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val containerWidth = maxWidth
                val horizontalGap = 12.dp
                // Compute button size dynamically between 48.dp and 62.dp for perfect fit
                val calculatedSize = ((containerWidth - (horizontalGap * 2)) / 3) - 8.dp
                val buttonSize = calculatedSize.coerceIn(48.dp, 62.dp)

                val numberRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    numberRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { key ->
                                KeypadButton(
                                    label = key,
                                    buttonSize = buttonSize,
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
}

@Composable
private fun KeypadButton(
    label: String,
    buttonSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Distinct styling for Clear (C) and Backspace (DEL)
    val baseBgColor = when (label) {
        "C" -> Color(0xFF1E293B)
        "DEL" -> Color(0xFF1E293B)
        else -> Color(0xFF1E2D4A) // Lighter navy blue for number buttons
    }

    val activeBgColor = if (isPressed) {
        when (label) {
            "C" -> AccentGold.copy(alpha = 0.35f)
            "DEL" -> AccentRed.copy(alpha = 0.35f)
            else -> AccentCyan.copy(alpha = 0.4f)
        }
    } else baseBgColor

    val borderColor = when (label) {
        "C" -> AccentGold.copy(alpha = 0.4f)
        "DEL" -> AccentRed.copy(alpha = 0.4f)
        else -> Color(0xFF334155)
    }

    Surface(
        modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("pin_key_$label"),
        shape = CircleShape,
        color = activeBgColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (label) {
                "C" -> Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = AccentGold,
                    modifier = Modifier.size((buttonSize.value * 0.42f).dp)
                )
                "DEL" -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = AccentRed,
                    modifier = Modifier.size((buttonSize.value * 0.42f).dp)
                )
                else -> Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        fontSize = (buttonSize.value * 0.4f).sp
                    )
                )
            }
        }
    }
}
