package com.tonmoy.lexi.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tonmoy.lexi.R
import com.tonmoy.lexi.container
import com.tonmoy.lexi.quickadd.QuickAddActivity
import com.tonmoy.lexi.ui.detail.WordDetailScreen
import com.tonmoy.lexi.ui.flashcards.FlashcardsScreen
import com.tonmoy.lexi.ui.home.HomeScreen
import com.tonmoy.lexi.ui.intro.IntroMode
import com.tonmoy.lexi.ui.intro.IntroPrefs
import com.tonmoy.lexi.ui.intro.IntroScreen
import com.tonmoy.lexi.ui.theme.LexiTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Word to open, set from the widget or the quick-add sheet. */
    private val pendingWordId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // The manifest gives this activity the brand-coloured launch theme so the system launch
        // screen matches the intro's first frame; switch to the app theme before any UI exists.
        setTheme(R.style.Theme_Lexi)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) consumeIntent(intent)
        val repository = container.repository

        // Play the intro on a fresh launch only: not after rotation, and not when we were opened
        // to show a specific word (the user is mid-task).
        val initialIntro = when {
            savedInstanceState != null || pendingWordId.value != null -> null
            !IntroPrefs.isDone(this) -> IntroMode.Full
            else -> IntroMode.Splash
        }

        setContent {
            LexiTheme {
                val nav = rememberNavController()
                val scope = rememberCoroutineScope()
                val openId = pendingWordId.value
                LaunchedEffect(openId) {
                    if (openId != null) {
                        nav.navigate("word/$openId") { launchSingleTop = true }
                        pendingWordId.value = null
                    }
                }

                var intro by remember { mutableStateOf(initialIntro) }
                // 0 while the intro covers the app, 1 once it has opened onto it.
                val introExit = remember { Animatable(if (initialIntro == null) 1f else 0f) }

                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            // The app settles from slightly enlarged to rest as the intro opens.
                            .graphicsLayer {
                                val s = 1.06f - 0.06f * introExit.value
                                scaleX = s
                                scaleY = s
                            }
                    ) {
                        NavHost(
                            navController = nav,
                            startDestination = "home",
                            enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
                            exitTransition = { fadeOut() + scaleOut(targetScale = 0.96f) },
                            popEnterTransition = { fadeIn() + scaleIn(initialScale = 0.96f) },
                            popExitTransition = { slideOutHorizontally { it / 4 } + fadeOut() },
                        ) {
                            composable("home") {
                                HomeScreen(
                                    repository = repository,
                                    onOpenWord = { nav.navigate("word/$it") },
                                    onOpenFlashcards = { nav.navigate("flashcards") },
                                    onAddWord = {
                                        startActivity(Intent(this@MainActivity, QuickAddActivity::class.java))
                                    },
                                    onReplayIntro = {
                                        scope.launch {
                                            introExit.snapTo(0f)
                                            intro = IntroMode.Full
                                        }
                                    },
                                )
                            }
                            composable(
                                "word/{id}",
                                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                            ) { entry ->
                                WordDetailScreen(
                                    id = entry.arguments!!.getLong("id"),
                                    repository = repository,
                                    onBack = { nav.popBackStack() },
                                )
                            }
                            composable("flashcards") {
                                FlashcardsScreen(repository = repository, onBack = { nav.popBackStack() })
                            }
                        }
                    }

                    intro?.let { mode ->
                        IntroScreen(
                            mode = mode,
                            exit = introExit,
                            onFinished = {
                                if (mode == IntroMode.Full) IntroPrefs.markDone(this@MainActivity)
                                intro = null
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_WORD_ID, -1L) ?: -1L
        if (id > 0) pendingWordId.value = id
    }

    companion object {
        const val EXTRA_WORD_ID = "word_id"
    }
}
