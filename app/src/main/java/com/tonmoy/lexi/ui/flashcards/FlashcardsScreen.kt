package com.tonmoy.lexi.ui.flashcards

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tonmoy.lexi.data.WordEntity
import com.tonmoy.lexi.data.WordRepository
import com.tonmoy.lexi.ui.home.PosTag
import com.tonmoy.lexi.ui.theme.WordFamily
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(repository: WordRepository, onBack: () -> Unit) {
    var allWords by remember { mutableStateOf<List<WordEntity>?>(null) }
    var includeLearned by remember { mutableStateOf(false) }
    var session by remember { mutableIntStateOf(0) }

    // Snapshot the deck once per session so it doesn't reshuffle as reviews update rows.
    LaunchedEffect(session) { allWords = repository.observeAll().first() }

    val deck = remember(allWords, includeLearned, session) {
        allWords.orEmpty().filter { includeLearned || !it.learned }.shuffled()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                actions = {
                    FilterChip(
                        selected = includeLearned,
                        onClick = { includeLearned = !includeLearned },
                        label = { Text("Include learned") },
                        shape = CircleShape,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).navigationBarsPadding()) {
            when {
                allWords == null -> Unit
                deck.isEmpty() -> EmptyDeck(hasWords = allWords!!.isNotEmpty())
                else -> key(deck) {
                    Session(deck, repository, onRestart = { session++ })
                }
            }
        }
    }
}

@Composable
private fun Session(deck: List<WordEntity>, repository: WordRepository, onRestart: () -> Unit) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var index by remember { mutableIntStateOf(0) }
    var known by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(index / deck.size.toFloat(), label = "progress")

    fun answer(gotIt: Boolean) {
        val word = deck[index]
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        scope.launch { repository.recordReview(word.id, gotIt) }
        if (gotIt) known++
        flipped = false
        index++
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (index < deck.size) "${index + 1} of ${deck.size}" else "Done",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        AnimatedContent(
            targetState = index,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "card",
        ) { i ->
            if (i >= deck.size) {
                Finished(known = known, total = deck.size, onRestart = onRestart)
            } else {
                Column {
                    FlipCard(
                        word = deck[i],
                        flipped = flipped && i == index,
                        onFlip = { flipped = !flipped },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { answer(false) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Icon(Icons.Rounded.Replay, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Again")
                        }
                        Button(
                            onClick = { answer(true) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Icon(Icons.Rounded.Check, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Got it")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FlipCard(word: WordEntity, flipped: Boolean, onFlip: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(450), label = "flip")
    val showBack = rotation > 90f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFlip,
            ),
        shape = RoundedCornerShape(32.dp),
        color = if (showBack) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // Counter-rotate the back so its text isn't mirrored.
                .graphicsLayer { rotationY = if (showBack) 180f else 0f }
                .padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (!showBack) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(word.display, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
                    word.phonetic?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(32.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.TouchApp, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text("Tap to reveal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    word.partOfSpeech?.let { PosTag(it) }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        word.definition ?: word.note.ifBlank { "No meaning saved yet" },
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = MaterialTheme.typography.bodyLarge.fontFamily),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    word.example?.let {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "“$it”",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = WordFamily),
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Finished(known: Int, total: Int, onRestart: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        AnimatedVisibility(shown, enter = scaleIn() + fadeIn()) {
            Icon(
                Icons.Rounded.EmojiEvents, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Session complete", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "You knew $known of $total",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRestart, shape = RoundedCornerShape(16.dp)) { Text("Review again") }
    }
}

@Composable
private fun EmptyDeck(hasWords: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.EmojiEvents, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(
            if (hasWords) "Everything's learned!" else "No words yet",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasWords) "Turn on “Include learned” to review them again."
            else "Save a few words first, then come back to review.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
