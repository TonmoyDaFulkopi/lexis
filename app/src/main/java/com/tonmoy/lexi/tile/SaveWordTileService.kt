package com.tonmoy.lexi.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tonmoy.lexi.R
import com.tonmoy.lexi.container
import com.tonmoy.lexi.quickadd.QuickAddActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SaveWordTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            val count = container.repository.count()
            qsTile?.apply {
                state = Tile.STATE_INACTIVE
                label = getString(R.string.tile_label)
                icon = Icon.createWithResource(this@SaveWordTileService, R.drawable.ic_tile)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    subtitle = if (count == 1) "1 word" else "$count words"
                }
                updateTile()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun { launchQuickAdd() } else launchQuickAdd()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchQuickAdd() {
        val intent = Intent(this, QuickAddActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
