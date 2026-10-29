package com.tonmoy.lexi.quickadd

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tonmoy.lexi.container
import com.tonmoy.lexi.data.WordRepository
import com.tonmoy.lexi.ui.MainActivity
import com.tonmoy.lexi.ui.theme.LexiTheme
import com.tonmoy.lexi.ui.theme.WordFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Floating "save a word" sheet launched from the Quick Settings tile (and the in-app FAB).
 * Translucent so it appears on top of whatever app the user was reading.
 */
class QuickAddActivity : ComponentActivity() {

    /** Android 10+ only allows clipboard reads while we hold window focus, so we fill this then. */
    private val clipboardWord = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
        setContent {
            LexiTheme {
                QuickAddScreen(
                    clipboardWord = clipboardWord.value,
                    onSave = { container.repository.save(it) },
                    onOpenWord = { id ->
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .putExtra(MainActivity.EXTRA_WORD_ID, id)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                        finish()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && clipboardWord.value == null) {
            val clip = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
            val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
            if (text != null && WordRepository.looksLikeWord(text)) {
                clipboardWord.value = WordRepository.clean(text)
            }
        }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}

private sealed interface SheetState {
    data object Editing : SheetState
    data class Saved(val word: String, val id: Long, val existed: Boolean) : SheetState
}

@Composable
private fun QuickAddScreen(
    clipboardWord: String?,
    onSave: suspend (String) -> WordRepository.SaveResult,
    onOpenWord: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    var sheet by remember { mutableStateOf<SheetState>(SheetState.Editing) }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var prefilledFromClipboard by remember { mutableStateOf(false) }

    fun close() {
        visible.targetState = false
    }

    // Finish the activity once the exit animation has played.
    LaunchedEffect(visible.currentState, visible.targetState) {
        if (!visible.targetState && !visible.currentState) onDismiss()
    }

    LaunchedEffect(clipboardWord) {
        if (clipboardWord != null && field.text.isEmpty()) {
            // Select everything so typing simply replaces the suggestion.
            field = TextFieldValue(clipboardWord, TextRange(0, clipboardWord.length))
            prefilledFromClipboard = true
        }
    }

    fun submit() {
        val text = WordRepository.clean(field.text)
        if (text.isEmpty() || sheet != SheetState.Editing) return
        scope.launch {
            val result = onSave(text)
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.LONG_PRESS
            )
            sheet = SheetState.Saved(text, result.id, result is WordRepository.SaveResult.AlreadySaved)
            delay(1600)
            close()
        }
    }

    BackHandler { close() }

    Box(Modifier.fillMaxSize()) {
        // Scrim
        AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { close() }
            )
        }

        AnimatedVisibility(
            visibleState = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { it } +
                fadeIn(),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(220)),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(12.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp,
            ) {
                AnimatedContent(
                    targetState = sheet,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                    label = "sheet",
                ) { state ->
                    when (state) {
                        SheetState.Editing -> EditingContent(
                            field = field,
                            onFieldChange = {
                                field = it
                                if (it.text != clipboardWord) prefilledFromClipboard = false
                            },
                            fromClipboard = prefilledFromClipboard,
                            clipboardWord = clipboardWord,
                            onUseClipboard = { w ->
                                field = TextFieldValue(w, TextRange(w.length))
                                prefilledFromClipboard = true
                            },
                            onSubmit = ::submit,
                        )
                        is SheetState.Saved -> SavedContent(state, onOpen = { onOpenWord(state.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EditingContent(
    field: TextFieldValue,
    onFieldChange: (TextFieldValue) -> Unit,
    fromClipboard: Boolean,
    clipboardWord: String?,
    onUseClipboard: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        delay(120)
        focus.requestFocus()
        keyboard?.show()
    }

    Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Bookmark, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Save a new word", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "We'll look up the meaning for you",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = field,
            onValueChange = onFieldChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
            placeholder = { Text("e.g. serendipity", fontFamily = WordFamily) },
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = WordFamily),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                fromClipboard -> AssistChip(
                    onClick = {},
                    label = { Text("From clipboard") },
                    leadingIcon = { Icon(Icons.Rounded.ContentPaste, null, Modifier.size(AssistChipDefaults.IconSize)) },
                )
                clipboardWord != null -> AssistChip(
                    onClick = { onUseClipboard(clipboardWord) },
                    label = { Text("Paste “$clipboardWord”", maxLines = 1) },
                    leadingIcon = { Icon(Icons.Rounded.ContentPaste, null, Modifier.size(AssistChipDefaults.IconSize)) },
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onSubmit,
                enabled = field.text.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun SavedContent(state: SheetState.Saved, onOpen: () -> Unit) {
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .scale(pop.value)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(state.word, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = WordFamily))
        Text(
            if (state.existed) "Already in your collection" else "Saved · fetching meaning…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpen) { Text("View word") }
    }
}
