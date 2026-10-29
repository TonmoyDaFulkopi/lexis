package com.tonmoy.lexi.ui.detail

import android.speech.tts.TextToSpeech
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonmoy.lexi.data.LookupState
import com.tonmoy.lexi.data.WordEntity
import com.tonmoy.lexi.data.WordRepository
import com.tonmoy.lexi.ui.home.SyncButton
import com.tonmoy.lexi.ui.theme.WordFamily
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(id: Long, repository: WordRepository, onBack: () -> Unit) {
    val word by remember(id) { repository.observe(id) }.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val speak = rememberSpeaker()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                actions = {
                    word?.let { w ->
                        SyncButton(
                            syncing = w.lookupState == LookupState.PENDING,
                            onClick = { scope.launch { repository.resync(w.id) } },
                        )
                    }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.DeleteOutline, "Delete") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        val w = word ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Text(w.display, style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { speak(w.display) }) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Pronounce")
                }
                w.phonetic?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                FilterChip(
                    selected = w.learned,
                    onClick = { scope.launch { repository.setLearned(w.id, !w.learned) } },
                    label = { Text(if (w.learned) "Learned" else "Mark learned") },
                    leadingIcon = {
                        Icon(
                            if (w.learned) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            null, Modifier.size(18.dp),
                        )
                    },
                    shape = CircleShape,
                )
            }
            Spacer(Modifier.height(24.dp))

            // Keep showing an existing meaning while a resync is in flight.
            val shown = if (w.senses.isNotEmpty()) LookupState.FOUND else w.lookupState
            AnimatedContent(
                targetState = shown,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "lookup",
            ) { state ->
                when (state) {
                    LookupState.PENDING -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 24.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Looking up meaning…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LookupState.FOUND -> Senses(w)
                    LookupState.NOT_FOUND -> StatusMessage(
                        Icons.Rounded.SearchOff,
                        "No dictionary entry for this one. Write your own meaning in the note below.",
                        onRetry = { scope.launch { repository.resync(w.id) } },
                    )
                    LookupState.FAILED -> StatusMessage(
                        Icons.Rounded.CloudOff,
                        "Couldn't reach the dictionary. Check your connection.",
                        onRetry = { scope.launch { repository.resync(w.id) } },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            NoteField(w, repository)
            Spacer(Modifier.height(16.dp))
            Text(
                "Saved " + DateUtils.getRelativeTimeSpanString(w.createdAt) +
                    if (w.reviewCount > 0) " · reviewed ${w.reviewCount}×" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Delete “${w.display}”?") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        scope.launch { repository.delete(w) }
                        onBack()
                    }) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Senses(word: WordEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        word.senses.forEach { sense ->
            Column {
                Text(
                    sense.partOfSpeech,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(12.dp))
                sense.definitions.forEachIndexed { i, def ->
                    Row(Modifier.padding(bottom = 14.dp)) {
                        Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp),
                        )
                        Column {
                            Text(def.text, style = MaterialTheme.typography.bodyLarge)
                            def.example?.let {
                                Spacer(Modifier.height(6.dp))
                                Row {
                                    Box(
                                        Modifier
                                            .width(3.dp)
                                            .height(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "“$it”",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = WordFamily),
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                if (sense.synonyms.isNotEmpty()) {
                    Text("Similar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sense.synonyms.forEach { SuggestionChip(onClick = {}, label = { Text(it) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(icon: ImageVector, message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onRetry) {
            Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Try again")
        }
    }
}

@Composable
private fun NoteField(word: WordEntity, repository: WordRepository) {
    val scope = rememberCoroutineScope()
    var note by remember(word.id) { mutableStateOf(word.note) }
    OutlinedTextField(
        value = note,
        onValueChange = {
            note = it
            scope.launch { repository.updateNote(word.id, it) }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("My note") },
        placeholder = { Text("Where you found it, your own meaning, a sentence…") },
        minLines = 3,
        shape = RoundedCornerShape(18.dp),
    )
}

/** Text-to-speech bound to the composable's lifetime. */
@Composable
private fun rememberSpeaker(): (String) -> Unit {
    val context = LocalContext.current
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) engine?.language = Locale.US
        }
        engine
    }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }
    return { text -> tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lexi") }
}
