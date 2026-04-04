package com.bothybag.app.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bothybag.app.data.BothyRepository
import com.bothybag.app.data.NewsItem
import com.bothybag.app.data.NewsRepository
import com.bothybag.app.data.VisitedRepository
import com.bothybag.app.ui.components.BothyTypeBadge
import com.bothybag.app.ui.components.StatusBadge
import com.bothybag.app.ui.theme.AmberDark
import com.bothybag.app.ui.theme.GreenVisited

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BothyDetailScreen(
    bothyId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bothyRepo = remember { BothyRepository(context) }
    val visitedRepo = remember { VisitedRepository(context) }
    val newsRepo = remember { NewsRepository(context) }
    val bothy = remember { bothyRepo.getBothyById(bothyId) }
    val newsItems = remember { newsRepo.getNewsForBothy(bothyId) }

    if (bothy == null) {
        Text("Bothy not found", modifier = Modifier.padding(16.dp))
        return
    }

    var isVisited by remember { mutableStateOf(visitedRepo.isVisited(bothyId)) }
    var visitedDate by remember { mutableStateOf(visitedRepo.getVisitedDate(bothyId)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bothy.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mark as Visited button - bottom left, 1/3 width
                Button(
                    onClick = {
                        visitedRepo.toggleVisited(bothyId)
                        isVisited = !isVisited
                        visitedDate = visitedRepo.getVisitedDate(bothyId)
                    },
                    modifier = Modifier
                        .weight(1f / 3f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVisited) GreenVisited else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isVisited) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isVisited) "Visited" else "Visit",
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.weight(1f / 3f))

                // Navigate button - bottom right, 1/3 width
                Button(
                    onClick = {
                        val uri = Uri.parse(
                            "google.navigation:q=${bothy.latitude},${bothy.longitude}&mode=w"
                        )
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            val webUri = Uri.parse(
                                "https://www.google.com/maps/dir/?api=1&destination=${bothy.latitude},${bothy.longitude}&travelmode=walking"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    },
                    modifier = Modifier
                        .weight(1f / 3f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Navigate", fontSize = 14.sp, maxLines = 1)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Badges row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BothyTypeBadge(isMba = bothy.isMba)
                StatusBadge(status = bothy.status)
            }

            // Latest updates section (news matched to this bothy)
            if (newsItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                NewsUpdatesSection(newsItems = newsItems)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Single info block
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    bothy.region?.takeIf { it != "Unknown" }?.let {
                        InfoRow("Region", it)
                    }

                    bothy.elevationM?.let {
                        InfoRow("Elevation", "${it}m (${bothy.elevationFt}ft)")
                    }

                    bothy.osGridRef?.let {
                        InfoRow("OS Grid Ref", it)
                    }

                    bothy.fireInfo?.let {
                        InfoRow("Fire/Stove", it)
                    }

                    bothy.sizeInfo?.let {
                        InfoRow("Size", it)
                    }

                    if (bothy.bothyFeatures.isNotEmpty()) {
                        InfoRow("Features", bothy.bothyFeatures.joinToString(" • "))
                    }
                }
            }

            // Description text (no card border)
            bothy.description?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (bothy.isMba && bothy.mbaUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bothy.mbaUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Public, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MBA Website")
                }
            }

            // Source info
            Spacer(modifier = Modifier.height(16.dp))
            bothy.source?.let {
                Text(
                    text = "Source: $it",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun NewsUpdatesSection(newsItems: List<NewsItem>) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AmberDark.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Article,
                    contentDescription = null,
                    tint = AmberDark,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    "Latest updates",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            newsItems.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                            context.startActivity(intent)
                        }
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.pubDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                if (item != newsItems.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}
