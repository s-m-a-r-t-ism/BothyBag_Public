package com.bothybag.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bothybag.app.ui.theme.AmberDark
import com.bothybag.app.ui.theme.GreenVisited
import com.bothybag.app.ui.theme.GreyNonMba
import com.bothybag.app.ui.theme.RedClosed

@Composable
fun BothyTypeBadge(isMba: Boolean, modifier: Modifier = Modifier) {
    val bgColor = if (isMba) AmberDark else GreyNonMba
    val text = if (isMba) "MBA" else "Non-MBA"
    Badge(text = text, backgroundColor = bgColor, modifier = modifier)
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (text, bgColor) = when (status.lowercase()) {
        "open" -> "Open" to GreenVisited
        "closed" -> "Closed" to RedClosed
        "permanently_closed" -> "Permanently closed" to RedClosed
        "seasonal" -> "Seasonal" to AmberDark
        else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() } to GreyNonMba
    }
    Badge(text = text, backgroundColor = bgColor, modifier = modifier)
}

@Composable
fun PrivateBadge(modifier: Modifier = Modifier) {
    Badge(text = "Private", backgroundColor = RedClosed, modifier = modifier)
}

@Composable
private fun Badge(
    text: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
