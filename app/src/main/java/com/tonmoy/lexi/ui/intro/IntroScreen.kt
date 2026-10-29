package com.tonmoy.lexi.ui.intro

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.tonmoy.lexi.ui.theme.WordFamily
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object IntroPrefs {
    private const val PREFS = "lexi_prefs"
    private const val KEY_DONE = "intro_done"

    fun isDone(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)

    fun markDone(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_DONE, true) }
}

private val IntroTop = Color(0xFF041A2C)
private val IntroBottom = Color(0xFF0E6F86)
private val Brand = SpineInk

/** The name as the logo spells it: lexis, Greek for "word", written with a schwa. */
private const val WORDMARK = "ləxis"

private data class Step(val icon: ImageVector, val title: String, val body: String)

private val steps = listOf(
    Step(Icons.Rounded.ContentCopy, "Spot a word", "Copy it from any app you're reading"),
    Step(Icons.Rounded.TouchApp, "Swipe down, tap “Save word”", "Straight from your quick settings"),
    Step(Icons.Rounded.AutoStories, "Make it yours", "Meaning, pronunciation and flashcards"),
)

enum class IntroMode {
    /** First launch: the opening, then how-it-works cards and "Get started". */
    Full,
    /** Every later launch: the opening only, then straight into the app. */
    Splash,
}

/** Exit is a circle opening outward from this point, so the app shows through the brand screen. */
private const val EXIT_MS = 620
private val ExitEasing = CubicBezierEasing(0.45f, 0f, 0.15f, 1f)

/**
 * The Spine bookmark drops in like a ribbon settling into a book, "ləxis" pops up its spine
 * letter by letter, then the name and tagline arrive. In [IntroMode.Splash] it then opens onto
 * the app; in [IntroMode.Full] the logo lifts away to reveal how the app works first.
 *
 * [exit] runs 0 → 1 as the screen opens; the caller uses it to settle the app in underneath.
 * Tapping skips ahead.
 */
@Composable
fun IntroScreen(
    mode: IntroMode,
    exit: Animatable<Float, AnimationVector1D>,
    onFinished: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    SystemBarsOnDark()

    val background = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.7f) }
    val logoTilt = remember { Animatable(-8f) }
    val logoAlpha = remember { Animatable(0f) }
    val drop = remember { Animatable(-1f) }
    val spineLetters = remember { List(WORDMARK.length) { Animatable(0f) } }
    val letters = remember { List(WORDMARK.length) { Animatable(0f) } }
    val tagline = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    val stepAnims = remember { List(steps.size) { Animatable(0f) } }
    val button = remember { Animatable(0f) }
    var revealed by remember { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(false) }
    var markCenter by remember { mutableStateOf(Offset.Unspecified) }
    var buttonCenter by remember { mutableStateOf(Offset.Unspecified) }
    var holeCenter by remember { mutableStateOf(Offset.Unspecified) }

    fun finish(from: Offset) {
        if (exiting) return
        exiting = true
        holeCenter = from
        scope.launch {
            exit.animateTo(1f, tween(EXIT_MS, easing = ExitEasing))
            onFinished()
        }
    }

    /** Full mode: lift the logo and bring in the how-it-works cards. */
    suspend fun reveal() = coroutineScope {
        if (revealed) return@coroutineScope
        revealed = true
        launch { lift.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)) }
        delay(120)
        stepAnims.forEachIndexed { i, a ->
            launch {
                delay(i * 70L)
                a.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
            }
        }
        launch {
            delay(steps.size * 70L + 80)
            button.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    /** Snap the opening to its end state so a tap never waits on it. */
    fun completeOpening() {
        val quick = tween<Float>(150)
        scope.launch { background.animateTo(1f, quick) }
        scope.launch { logoScale.animateTo(1f, quick) }
        scope.launch { logoTilt.animateTo(0f, quick) }
        scope.launch { logoAlpha.animateTo(1f, quick) }
        scope.launch { drop.animateTo(0f, quick) }
        (spineLetters + letters).forEach { scope.launch { it.animateTo(1f, quick) } }
        scope.launch { tagline.animateTo(1f, quick) }
    }

    fun skip() {
        when (mode) {
            IntroMode.Splash -> finish(markCenter)
            IntroMode.Full -> if (!revealed) {
                completeOpening()
                scope.launch { reveal() }
            }
        }
    }

    LaunchedEffect(Unit) {
        // Let the first frames of a cold start (inflating the app underneath) pass before moving,
        // so the drop never stutters.
        repeat(2) { withFrameNanos { } }
        val opening = launch {
            // The system launch screen is the solid top colour; ease the gradient in from it.
            launch { background.animateTo(1f, tween(500)) }
            launch { logoAlpha.animateTo(1f, tween(140)) }
            // Ribbon falls into place with a little bounce.
            launch { drop.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
            launch { logoScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) }
            launch { logoTilt.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow)) }
            // l, ə, x, i, s pop up the spine from the bottom.
            spineLetters.forEachIndexed { i, a ->
                launch {
                    delay(260L + i * 75L)
                    a.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
                }
            }
            letters.forEachIndexed { i, a ->
                launch {
                    delay(560L + i * 55L)
                    a.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
                }
            }
            launch {
                delay(860)
                tagline.animateTo(1f, tween(350))
            }
        }
        when (mode) {
            IntroMode.Splash -> {
                // Hold on the finished tagline just long enough to read it, then open.
                delay(860L + 350L + 260L)
                finish(markCenter)
            }
            IntroMode.Full -> {
                opening.join()
                delay(450)
                reveal()
            }
        }
    }

    BackHandler { if (mode == IntroMode.Splash) finish(markCenter) else skip() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // Offscreen so the exit circle can punch a transparent hole through everything.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val p = exit.value
                if (p > 0f) {
                    val c = if (holeCenter.isSpecified) holeCenter else center
                    // Far enough to clear the farthest corner from the centre.
                    val maxR = listOf(
                        Offset.Zero, Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height),
                    ).maxOf { (it - c).getDistance() }
                    drawCircle(Color.Black, radius = maxR * p, center = c, blendMode = BlendMode.Clear)
                }
            }
            .drawBehind {
                drawRect(IntroTop)
                drawRect(Brush.verticalGradient(listOf(IntroTop, IntroBottom)), alpha = background.value)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = ::skip,
            ),
    ) {
        val liftPx = with(LocalDensity.current) { (maxHeight * 0.24f).toPx() }

        FloatingGlyphs(Modifier.graphicsLayer { alpha = background.value })

        // Logo + wordmark. On exit it swells slightly and fades, as if passing the camera.
        Column(
            Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    val e = exit.value
                    translationY = -liftPx * lift.value
                    val s = (1f - 0.18f * lift.value) * (1f + 0.12f * e)
                    scaleX = s
                    scaleY = s
                    alpha = (1f - e * 2.2f).coerceIn(0f, 1f)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.onGloballyPositioned { markCenter = it.boundsInRoot().center },
            ) {
                // Soft glow behind the mark
                Box(
                    Modifier
                        .size(180.dp)
                        .graphicsLayer { alpha = logoAlpha.value * 0.55f }
                        .background(
                            Brush.radialGradient(listOf(Color(0xFF3CC6D8).copy(alpha = 0.55f), Color.Transparent)),
                            CircleShape,
                        )
                )
                SpineMark(
                    drop = { drop.value },
                    letter = { i -> spineLetters[i].value },
                    modifier = Modifier
                        .size(132.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            rotationZ = logoTilt.value
                            alpha = logoAlpha.value
                        },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                WORDMARK.forEachIndexed { i, c ->
                    Text(
                        c.toString(),
                        color = Color.White,
                        fontFamily = WordFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 52.sp,
                        modifier = Modifier.graphicsLayer {
                            val v = letters[i].value
                            alpha = v.coerceIn(0f, 1f)
                            translationY = (1f - v) * 28.dp.toPx()
                        },
                    )
                }
            }
            Text(
                "Words worth keeping.",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.graphicsLayer {
                    alpha = tagline.value
                    translationY = (1f - tagline.value) * 10.dp.toPx()
                },
            )
        }

        if (mode == IntroMode.Full) {
            // How it works + CTA
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .graphicsLayer { alpha = (1f - exit.value * 2.2f).coerceIn(0f, 1f) },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                steps.forEachIndexed { i, step ->
                    StepRow(
                        step,
                        Modifier.graphicsLayer {
                            val v = stepAnims[i].value
                            alpha = v.coerceIn(0f, 1f)
                            translationY = (1f - v) * 40.dp.toPx()
                        },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { finish(buttonCenter) },
                    enabled = revealed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .onGloballyPositioned { buttonCenter = it.boundsInRoot().center }
                        .graphicsLayer {
                            val v = button.value
                            alpha = v.coerceIn(0f, 1f)
                            translationY = (1f - v) * 48.dp.toPx()
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Brand,
                        disabledContainerColor = Color.White,
                        disabledContentColor = Brand,
                    ),
                ) {
                    Text("Get started", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StepRow(step: Step, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(step.icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(step.title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(step.body, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Faint serif letters drifting in the background for texture. */
@Composable
private fun FloatingGlyphs(modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "glyphs").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val glyphs = remember {
        listOf(
            Triple("a", 0.12f to 0.10f, 64),
            Triple("ə", 0.78f to 0.14f, 80),
            Triple("ʃ", 0.86f to 0.42f, 56),
            Triple("é", 0.08f to 0.46f, 72),
            Triple("ŋ", 0.30f to 0.26f, 44),
            Triple("ö", 0.64f to 0.30f, 48),
            Triple("ð", 0.20f to 0.62f, 52),
            Triple("ç", 0.80f to 0.66f, 60),
        )
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        glyphs.forEachIndexed { i, (ch, pos, size) ->
            val dir = if (i % 2 == 0) 1f else -1f
            Text(
                ch,
                color = Color.White.copy(alpha = 0.07f),
                fontFamily = WordFamily,
                fontSize = size.sp,
                modifier = Modifier
                    .offset(x = maxWidth * pos.first, y = maxHeight * pos.second)
                    // Drift is applied while drawing, so it never recomposes.
                    .graphicsLayer { translationY = dir * 18.dp.toPx() * t },
            )
        }
    }
}

/** The intro is always dark, so force light system-bar icons while it's shown. */
@Composable
private fun SystemBarsOnDark() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val status = controller?.isAppearanceLightStatusBars
        val nav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (status != null) controller.isAppearanceLightStatusBars = status
            if (nav != null) controller.isAppearanceLightNavigationBars = nav
        }
    }
}
