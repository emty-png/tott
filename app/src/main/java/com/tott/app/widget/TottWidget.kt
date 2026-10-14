package com.tott.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.RunCallbackAction
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tott.app.MainActivity
import com.tott.app.data.TaskRepository
import com.tott.app.data.WorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ActionParameters.Key<Long>(KEY_TASK_ID)] ?: return
        try {
            val repo = EntryPointAccessors
                .fromApplication(context.applicationContext, WorkerEntryPoint::class.java)
                .repo()
            repo.toggle(id)
            TottWidget().update(context, glanceId)
        } catch (e: Exception) {
            // store unavailable; widget refreshes on next update cycle
        }
    }
}

class TottWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasks = try {
            val repo = EntryPointAccessors
                .fromApplication(context.applicationContext, WorkerEntryPoint::class.java)
                .repo()
            val today = LocalDate.now().toEpochDay()
            repo.all().filter { t ->
                !t.done && (if (t.dueDate >= 0) t.dueDate <= today else t.due == "Today")
            }.take(6)
        } catch (e: Exception) {
            emptyList()
        }
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).padding(12.dp)
                ) {
                    Text(
                        text = "tott · Today (${tasks.size})",
                        style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = GlanceTheme.colors.onSurface)
                    )
                    Spacer(GlanceModifier.height(6.dp))
                    if (tasks.isEmpty()) {
                        Text(
                            text = "Nothing due — enjoy the day.",
                            style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    } else {
                        tasks.forEach { t ->
                            Row(
                                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = t.title,
                                    maxLines = 1,
                                    style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurface),
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                Spacer(GlanceModifier.width(8.dp))
                                androidx.glance.Button(
                                    text = "✓",
                                    onClick = RunCallbackAction(
                                        ToggleTaskAction::class.java,
                                        actionParametersOf(ActionParameters.Key<Long>(KEY_TASK_ID) to t.id)
                                    )
                                )
                            }
                        }
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    androidx.glance.Button(
                        text = "Open tott",
                        onClick = actionStartActivity<MainActivity>()
                    )
                }
            }
        }
    }
}

class TottWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TottWidget()
}

private const val KEY_TASK_ID = "task_id"
