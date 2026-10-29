package com.tonmoy.lexi

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.tonmoy.lexi.data.DictionaryApi
import com.tonmoy.lexi.data.LexiDatabase
import com.tonmoy.lexi.data.WordRepository
import com.tonmoy.lexi.tile.SaveWordTileService
import com.tonmoy.lexi.widget.WordWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database = LexiDatabase.build(context)

    val repository = WordRepository(
        context = context,
        dao = database.wordDao(),
        api = DictionaryApi(),
        onWordsChanged = {
            WordWidget.refreshAll(context)
            TileService.requestListeningState(
                context, ComponentName(context, SaveWordTileService::class.java)
            )
        },
    )
}

class LexiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch { container.repository.resumePendingLookups() }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as LexiApp).container
