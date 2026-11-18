package com.bothybag.app.ui.detail

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bothybag.app.data.BothyRepository
import com.bothybag.app.data.NewsItem
import com.bothybag.app.data.NewsRepository
import com.bothybag.app.data.NotesRepository
import com.bothybag.app.data.VisitedRepository
import com.bothybag.app.ui.components.BothyTypeBadge
import com.bothybag.app.ui.components.PrivateBadge
import com.bothybag.app.ui.components.StatusBadge
import com.bothybag.app.ui.theme.AmberDark
import com.bothybag.app.ui.theme.GreenVisited
import com.bothybag.app.util.openUrlSafely
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BothyDetailScreen(
    bothyId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val bothyRepo = remember { BothyRepository(context) }
    val visitedRepo = remember { VisitedRepository(context) }
    val newsRepo = remember { NewsRepository(context) }
    val notesRepo = remember { NotesRepository(context) }
    val bothy = remember { bothyRepo.getBothyById(bothyId) }
    var newsItems by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    LaunchedEffect(bothyId) {
        val cached = newsRepo.getNewsForBothy(bothyId)
        if (cached.isNotEmpty()) {
            newsItems = cached
        } else {
            // News not cached yet — fetch it now (also populates cache for News screen)
            newsRepo.getNews(bothyRepo)
            newsItems = newsRepo.getNewsForBothy(bothyId)
        }
    }

    if (bothy == null) {
        Text("Bothy not found", modifier = Modifier.padding(16.dp))
        return
    }

    var isVisited by remember { mutableStateOf(visitedRepo.isVisited(bothyId)) }
    var visitedDate by remember { mutableStateOf(visitedRepo.getVisitedDate(bothyId)) }
    var noteText by remember { mutableStateOf(notesRepo.getNote(bothyId)) }
    var showDatePicker by remember { mutableStateOf(false) }

    val displayDateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
    }

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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Mark as Visited button - opens date picker, bottom left, 1/3 width
                Button(
                    onClick = {
                        if (isVisited) {
                            visitedRepo.markUnvisited(bothyId)
                            isVisited = false
                            visitedDate = null
                        } else {
                            showDatePicker = true
                        }
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

                Spacer(modifier = Modifier.weight(2f / 3f))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                }
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Badges row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BothyTypeBadge(isMba = bothy.isMba)
                // "Open" next to "Private" would contradict itself, so a private
                // bothy only shows its status when that status is not open
                if (!bothy.isPrivate || !bothy.isOpen) {
                    StatusBadge(status = bothy.status)
                }
                if (bothy.isPrivate) {
                    PrivateBadge()
                }
            }

            // Important notes (closures, seasonal restrictions, warnings)
            bothy.notes?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = note,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (bothy.isClosed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Visited date display
            if (isVisited && visitedDate != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val formattedDate = try {
                    LocalDate.parse(visitedDate).format(displayDateFormatter)
                } catch (e: Exception) {
                    visitedDate
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GreenVisited,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Visited: $formattedDate",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = GreenVisited
                    )
                }
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

                    InfoRow("Coordinates", "%.5f, %.5f".format(bothy.latitude, bothy.longitude))

                    bothy.accessInfo?.let {
                        InfoRow("Access", it)
                    }

                    bothy.fireInfo?.let {
                        InfoRow("Fire/Stove", it)
                    }

                    bothy.sizeInfo?.let {
                        InfoRow("Size", it)
                    }

                    if (!bothy.bothyFeatures.isNullOrEmpty()) {
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

            // Personal notes
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { newText ->
                    noteText = newText
                    notesRepo.saveNote(bothyId, newText)
                },
                label = { Text("Personal notes") },
                placeholder = { Text("e.g. Great spot, avoid in winter") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                trailingIcon = {
                    if (noteText.isNotEmpty()) {
                        IconButton(onClick = {
                            noteText = ""
                            notesRepo.saveNote(bothyId, "")
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear notes",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (bothy.isMba && bothy.mbaUrl != null) {
                    OutlinedButton(
                        onClick = { context.openUrlSafely(bothy.mbaUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MBA Website")
                    }
                }
                OutlinedButton(
                    onClick = {
                        val shareText = buildString {
                            append(bothy.name)
                            bothy.region?.takeIf { it != "Unknown" }?.let {
                                append(" — $it")
                            }
                            bothy.osGridRef?.let {
                                append(" — Grid ref: $it")
                            }
                            if (bothy.isMba && bothy.mbaUrl != null) {
                                append(" — More info: ${bothy.mbaUrl}")
                            }
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share bothy"))
                    },
                    modifier = if (bothy.isMba && bothy.mbaUrl != null) Modifier.weight(1f) else Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
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

    // Date picker dialog
    if (showDatePicker) {
        // The date picker works in UTC: a day is the millisecond of its UTC midnight.
        // Start on today's local date, expressed that way.
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Read it back in UTC too. Using the phone's time zone here
                            // gave the day before for anyone west of Greenwich.
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            visitedRepo.markVisitedOnDate(bothyId, selectedDate)
                            isVisited = true
                            visitedDate = selectedDate.toString()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
                            context.openUrlSafely(item.link)
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
