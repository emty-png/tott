package com.tott.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tott.app.data.TaskEntity
import com.tott.app.ui.icons.TottIcons2
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private fun tasksOn(tasks: List<TaskEntity>, date: LocalDate): List<TaskEntity> {
    val epoch = date.toEpochDay()
    val today = LocalDate.now()
    return tasks.filter { t ->
        if (t.dueDate >= 0) t.dueDate == epoch
        else {
            val label = when (date) {
                today -> "Today"
                today.plusDays(1) -> "Tomorrow"
                else -> null
            }
            label != null && t.due == label
        }
    }.sortedBy { it.dueTime }
}

@Composable
fun CalendarScreen(
    tasks: List<TaskEntity>,
    onToggle: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onAddOnDay: (Long) -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    val firstDow = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val firstOfMonth = month.atDay(1)
    val lead = (firstOfMonth.dayOfWeek.value - firstDow.value + 7) % 7
    val cells = remember(month) {
        val list = mutableListOf<LocalDate?>()
        repeat(lead) { list.add(null) }
        for (d in 1..month.lengthOfMonth()) list.add(month.atDay(d))
        while (list.size % 7 != 0) list.add(null)
        list
    }
    val dayTasks = remember(tasks, selected) { tasksOn(tasks, selected) }
    val undated = remember(tasks) { tasks.count { it.dueDate < 0 && !it.done } }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                DateTimeFormatter.ofPattern("MMMM yyyy").format(month),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { month = YearMonth.now(); selected = LocalDate.now() }) { Text("Today") }
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(TottIcons2.Back, contentDescription = "Previous month")
            }
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(TottIcons2.Forward, contentDescription = "Next month")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            for (i in 0..6) {
                val dow = DayOfWeek.of(((firstDow.value - 1 + i) % 7) + 1)
                Text(
                    dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Column {
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val isSel = date == selected
                            val isToday = date == LocalDate.now()
                            val dots = tasksOn(tasks, date).filter { !it.done }
                            Column(
                                Modifier.weight(1f).aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { selected = date },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "${date.dayOfMonth}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isSel -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    dots.take(3).forEach { t ->
                                        Box(
                                            Modifier.size(5.dp).clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSel -> MaterialTheme.colorScheme.onPrimary
                                                        t.pri == "high" -> MaterialTheme.colorScheme.error
                                                        else -> MaterialTheme.colorScheme.primary
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                DateTimeFormatter.ofPattern("EEE, MMM d").format(selected) + " · ${dayTasks.size}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            TextButton(onClick = { onAddOnDay(selected.toEpochDay()) }) { Text("+ Add") }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            if (dayTasks.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nothing due", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (undated > 0) "$undated undated open task(s)." else "Tap + Add to plan this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(dayTasks, key = { it.id }) { t ->
                    TaskRow(t, onToggle = { onToggle(t.id) }, onOpen = { onOpenDetail(t.id) })
                }
            }
        }
    }
}
