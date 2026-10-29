package com.tonmoy.lexi.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tonmoy.lexi.R
import com.tonmoy.lexi.container
import com.tonmoy.lexi.data.WordEntity
import com.tonmoy.lexi.quickadd.QuickAddActivity
import com.tonmoy.lexi.ui.MainActivity

class WordWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val word = context.container.repository.random()
        provideContent {
            GlanceTheme { Content(context, word) }
        }
    }

    @Composable
    private fun Content(context: Context, word: WordEntity?) {
        val open = if (word != null) {
            actionStartActivity(
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_WORD_ID, word.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        } else {
            actionStartActivity(Intent(context, QuickAddActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(24.dp)
                .padding(16.dp)
                .clickable(open),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ləxis",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (word != null) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_refresh),
                        contentDescription = "Another word",
                        modifier = GlanceModifier.size(20.dp).clickable(actionRunCallback<RefreshAction>()),
                    )
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            if (word == null) {
                Text(
                    "Save your first word",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 18.sp, fontFamily = FontFamily.Serif),
                )
                Text(
                    "Tap to add",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                )
            } else {
                Text(
                    word.display,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Serif,
                    ),
                )
                val meta = listOfNotNull(word.phonetic, word.partOfSpeech).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(meta, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, fontStyle = FontStyle.Italic))
                }
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    word.definition ?: word.note.ifBlank { "Meaning coming soon…" },
                    maxLines = 3,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
                )
            }
        }
    }

    companion object {
        suspend fun refreshAll(context: Context) = WordWidget().updateAll(context)
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WordWidget().update(context, glanceId)
    }
}

class WordWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WordWidget()
}
