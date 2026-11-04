package com.example.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRed

object CategoryIconHelper {

    fun getCategoryIcon(category: String, isIncome: Boolean = false): ImageVector {
        if (isIncome) {
            val lower = category.lowercase()
            return when {
                lower.contains("salary") || lower.contains("pay") -> Icons.Default.AccountBalanceWallet
                lower.contains("business") -> Icons.Default.BusinessCenter
                lower.contains("freelance") || lower.contains("project") -> Icons.Default.Laptop
                lower.contains("invest") || lower.contains("dividend") -> Icons.Default.TrendingUp
                lower.contains("bonus") || lower.contains("gift") -> Icons.Default.CardGiftcard
                else -> Icons.Default.Payments
            }
        }

        val lower = category.lowercase()
        return when {
            lower.contains("grocery") || lower.contains("groceries") -> Icons.Default.ShoppingCart
            lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") || lower.contains("snack") -> Icons.Default.Restaurant
            lower.contains("fastfood") || lower.contains("cafe") || lower.contains("coffee") -> Icons.Default.Fastfood
            lower.contains("rent") || lower.contains("flat") || lower.contains("apartment") -> Icons.Default.Apartment
            lower.contains("house") || lower.contains("home") -> Icons.Default.Home
            lower.contains("electric") || lower.contains("power") || lower.contains("current") || lower.contains("gas") -> Icons.Default.Bolt
            lower.contains("internet") || lower.contains("wifi") || lower.contains("broadband") || lower.contains("network") -> Icons.Default.Wifi
            lower.contains("transport") || lower.contains("car") || lower.contains("fuel") || lower.contains("bus") || lower.contains("taxi") -> Icons.Default.DirectionsCar
            lower.contains("med") || lower.contains("health") || lower.contains("doctor") || lower.contains("pharma") -> Icons.Default.LocalHospital
            lower.contains("movie") || lower.contains("entertain") || lower.contains("cinema") || lower.contains("fun") -> Icons.Default.Movie
            lower.contains("shop") || lower.contains("cloth") || lower.contains("wear") -> Icons.Default.ShoppingBag
            lower.contains("family") || lower.contains("kids") || lower.contains("child") -> Icons.Default.FamilyRestroom
            lower.contains("gift") || lower.contains("donation") || lower.contains("charity") -> Icons.Default.CardGiftcard
            lower.contains("travel") || lower.contains("flight") || lower.contains("tour") || lower.contains("vacation") -> Icons.Default.Flight
            lower.contains("fit") || lower.contains("gym") || lower.contains("sport") -> Icons.Default.FitnessCenter
            lower.contains("school") || lower.contains("college") || lower.contains("edu") || lower.contains("tuition") -> Icons.Default.School
            lower.contains("book") || lower.contains("study") -> Icons.Default.MenuBook
            lower.contains("pet") || lower.contains("cat") || lower.contains("dog") -> Icons.Default.Pets
            lower.contains("personal") -> Icons.Default.Person
            else -> Icons.Default.Category
        }
    }

    fun getCategoryColor(category: String, isIncome: Boolean = false): Color {
        if (isIncome) return AccentGreen

        val lower = category.lowercase()
        return when {
            lower.contains("grocery") || lower.contains("food") || lower.contains("dining") -> Color(0xFFE65100) // Deep Orange
            lower.contains("rent") || lower.contains("house") -> Color(0xFF1565C0) // Deep Blue
            lower.contains("electric") || lower.contains("power") || lower.contains("gas") -> Color(0xFFF57F17) // Amber
            lower.contains("internet") || lower.contains("wifi") -> Color(0xFF00838F) // Cyan Dark
            lower.contains("transport") || lower.contains("car") -> Color(0xFF2E7D32) // Forest Green
            lower.contains("med") || lower.contains("health") -> AccentRed
            lower.contains("movie") || lower.contains("entertain") -> AccentPurple
            lower.contains("shop") -> Color(0xFFAD1457) // Pink / Magenta
            lower.contains("family") -> Color(0xFF00695C) // Teal
            lower.contains("travel") -> Color(0xFF0277BD) // Light Blue Dark
            lower.contains("gift") -> AccentGold
            lower.contains("personal") -> Color(0xFF4527A0) // Indigo
            else -> AccentBlue
        }
    }
}

@Composable
fun CategoryIconBadge(
    category: String,
    isIncome: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val icon = CategoryIconHelper.getCategoryIcon(category, isIncome)
    val color = CategoryIconHelper.getCategoryColor(category, isIncome)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = color,
            modifier = Modifier.size(iconSize)
        )
    }
}
