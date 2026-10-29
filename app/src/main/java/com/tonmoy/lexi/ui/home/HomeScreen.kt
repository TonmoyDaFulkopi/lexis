package com.tonmoy.lexi.ui.home

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonmoy.lexi.R
import com.tonmoy.lexi.data.LookupState
import com.tonmoy.lexi.data.WordEntity
import com.tonmoy.lexi.data.WordRepository
import com.tonmoy.lexi.tile.SaveWordTileService
import kotlinx.coroutines.launch
import java.util.Calendar

private enum class Filter(val label: String) { All("All"), New("To learn"), Learned("Learned") }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    repository: WordRepository,
    onOpenWord: (Long) -> Unit,
    onOpenFlashcards: () -> Unit,
    onAddWord: () -> Unit,
    onReplayIntro: () -> Unit,
) {
    val words by remember { repository.observeAll() }.collectAsStateWithLifecycle(initialValue = null)
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(Filter.All) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val all = words.orEmpty()
    val visible = all.filter { w ->
        (query.isBlank() || w.word.contains(query.trim(), ignoreCase = true) ||
            w.definition?.contains(query.trim(), ignoreCase = true) == true) &&
            when (filter) {
                Filter.All -> true
                Filter.New -> !w.learned
                Filter.Learned -> w.learned
            }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    // Long-press the title to replay the first-launch intro.
                    Column(Modifier.combinedClickable(onClick = {}, onLongClick = onReplayIntro)) {
                        Text("Your words")
                        if (all.isNotEmpty()) {
                            val learned = all.count { it.learned }
                            Text(
                                "${all.size} saved · $learned learned",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (all.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                val n = repository.resyncAll()
                                snackbar.showSnackbar("Refreshing $n word${if (n == 1) "" else "s"}…")
                            }
                        }) {
                            Icon(Icons.Rounded.Sync, contentDescription = "Resync all")
                        }
                        IconButton(onClick = onOpenFlashcards) {
                            Icon(Icons.Rounded.Style, contentDescription = "Flashcards")
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddWord,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("New word") },
                shape = RoundedCornerShape(20.dp),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (words == null) return@Scaffold
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "tile-banner") { TileBanner() }

            if (all.isEmpty()) {
                item(key = "empty") { EmptyState() }
                return@LazyColumn
            }

            item(key = "search") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search words or meanings") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear") }
                            }
                        },
                        singleLine = true,
                        shape = CircleShape,
                        keyboardOptions = KeyboardOptions.Default,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Filter.entries.forEach { f ->
                            FilterChip(
                                selected = filter == f,
                                onClick = { filter = f },
                                label = { Text(f.label) },
                                shape = CircleShape,
                            )
                        }
                    }
                }
            }

            if (visible.isEmpty()) {
                item(key = "no-results") {
                    Text(
                        "No matches",
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            visible.groupBy { sectionOf(it.createdAt) }.forEach { (section, group) ->
                item(key = "header-$section") {
                    Text(
                        section,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp).animateItem(),
                    )
                }
                items(group, key = { it.id }) { word ->
                    SwipeToDelete(
                        modifier = Modifier.animateItem(),
                        onDelete = {
                            scope.launch {
                                repository.delete(word)
                                val result = snackbar.showSnackbar(
                                    "Deleted “${word.display}”", actionLabel = "Undo", withDismissAction = true,
                                )
                                if (result == SnackbarResult.ActionPerformed) repository.restore(word)
                            }
                        },
                    ) {
                        WordCard(
                            word,
                            onClick = { onOpenWord(word.id) },
                            onResync = { scope.launch { repository.resync(word.id) } },
                        )
                    }
                }
            }
        }
    }
}

private fun sectionOf(time: Long): String {
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return when {
        time >= startOfToday -> "Today"
        time >= startOfToday - DateUtils.DAY_IN_MILLIS -> "Yesterday"
        time >= startOfToday - 6 * DateUtils.DAY_IN_MILLIS -> "This week"
        time >= startOfToday - 30 * DateUtils.DAY_IN_MILLIS -> "This month"
        else -> "Earlier"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        },
        positionalThreshold = { it * 0.4f },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                if (state.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceContainer,
                label = "swipe-bg",
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) { content() }
}

@Composable
private fun WordCard(word: WordEntity, onClick: () -> Unit, onResync: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                ) {
                    Text(
                        word.display,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    word.phonetic?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
                if (word.learned) {
                    Icon(
                        Icons.Rounded.CheckCircle, "Learned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp).size(20.dp),
                    )
                }
                SyncButton(syncing = word.lookupState == LookupState.PENDING, onClick = onResync)
            }
            Column(Modifier.padding(end = 12.dp)) {
                when {
                    // Show what we have, even mid-resync.
                    word.definition != null -> Row(verticalAlignment = Alignment.Top) {
                        word.partOfSpeech?.let { PosTag(it) }
                        Text(
                            word.definition,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    word.lookupState == LookupState.PENDING -> ShimmerLines()
                    word.lookupState == LookupState.FAILED -> Text(
                        "Couldn't fetch the meaning. Tap ⟳ to try again",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> Text(
                        word.note.ifBlank { "No dictionary entry. Tap to add your own note" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = if (word.note.isBlank()) FontStyle.Italic else FontStyle.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

/** Refresh button whose icon spins while a lookup is in flight. */
@Composable
fun SyncButton(syncing: Boolean, onClick: () -> Unit) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(syncing) {
        if (syncing) {
            while (true) {
                rotation.animateTo(rotation.value + 360f, tween(900, easing = LinearEasing))
            }
        } else {
            // Finish the current turn rather than snapping back.
            val target = (rotation.value / 360f).let { kotlin.math.ceil(it) } * 360f
            rotation.animateTo(target, tween(300))
            rotation.snapTo(0f)
        }
    }
    IconButton(onClick = onClick, enabled = !syncing) {
        Icon(
            Icons.Rounded.Sync,
            contentDescription = if (syncing) "Syncing" else "Resync meaning",
            tint = if (syncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp).rotate(rotation.value),
        )
    }
}

@Composable
fun PosTag(pos: String) {
    Text(
        pos,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier
            .padding(end = 8.dp, top = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun ShimmerLines() {
    val alpha by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.35f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "shimmer-alpha",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.alpha(alpha)) {
        Box(Modifier.fillMaxWidth(0.9f).height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
        Box(Modifier.fillMaxWidth(0.6f).height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
    }
}

@Composable
private fun EmptyState() {
    val bob by rememberInfiniteTransition(label = "bob").animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "bob-y",
    )
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .offset(y = bob.dp)
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoStories, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Collect words as you find them", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Copy a word anywhere, pull down your quick settings and tap “Save word”. Lexi fetches the meaning for you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private const val PREFS = "lexi_prefs"
private const val KEY_TILE_BANNER_DONE = "tile_banner_done"

@Composable
private fun TileBanner() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var show by remember { mutableStateOf(!prefs.getBoolean(KEY_TILE_BANNER_DONE, false)) }
    var showHelp by remember { mutableStateOf(false) }

    fun done() {
        prefs.edit { putBoolean(KEY_TILE_BANNER_DONE, true) }
        show = false
    }

    AnimatedVisibility(show, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TouchApp, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Save words from anywhere",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = ::done) {
                        Icon(Icons.Rounded.Close, "Dismiss", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Text(
                    "Add the Lexi tile to your quick settings. One swipe down and a tap, and the word is saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val sbm = context.getSystemService(StatusBarManager::class.java)
                        sbm.requestAddTileService(
                            ComponentName(context, SaveWordTileService::class.java),
                            context.getString(R.string.tile_label),
                            Icon.createWithResource(context, R.drawable.ic_tile),
                            context.mainExecutor,
                        ) { result ->
                            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                                result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
                            ) done()
                        }
                    } else {
                        showHelp = true
                    }
                }) { Text("Add tile") }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = { TextButton(onClick = { showHelp = false; done() }) { Text("Got it") } },
            title = { Text("Add the tile") },
            text = {
                Text(
                    "1. Swipe down twice from the top of the screen\n" +
                        "2. Tap the pencil (edit) icon\n" +
                        "3. Drag “Save word” into your active tiles"
                )
            },
        )
    }
}
