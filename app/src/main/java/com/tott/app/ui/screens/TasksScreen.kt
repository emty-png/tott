package com.tott.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tott.app.data.TaskEntity
import com.tott.app.data.TaskFilter
import com.tott.app.data.TaskRepository
import com.tott.app.ui.components.GroupHeader
import com.tott.app.ui.components.HeroCard
import com.tott.app.ui.components.PriorityPill
import com.tott.app.ui.components.TottFilterChip
import com.tott.app.ui.icons.TottIcons2

@Composable
fun TasksScreen(
    allTasks: List<TaskEntity>,
    visibleTasks: List<TaskEntity>,
    query: String,
    searchVisible: Boolean,
    filter: TaskFilter,
    project: String?,
    onQuery: (String) -> Unit,
    onFilter: (TaskFilter) -> Unit,
    onClearProject: () -> Unit,
    onToggle: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (searchVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(TottIcons2.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                TextField(
                    value = query,
                    onValueChange = onQuery,
                    placeholder = { Text("Search tasks") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        val today = allTasks.filter { it.due == "Today" }
        val doneToday = today.count { it.done }
        HeroCard(open = today.size - doneToday, total = today.size)
        Spacer(Modifier.height(8.dp))

        if (project != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TottFilterChip(label = project, selected = true, onClick = onClearProject)
            }
            Spacer(Modifier.height(4.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            TottFilterChip("All", filter == TaskFilter.ALL) { onFilter(TaskFilter.ALL) }
            TottFilterChip("Today", filter == TaskFilter.TODAY) { onFilter(TaskFilter.TODAY) }
            TottFilterChip("High priority", filter == TaskFilter.HIGH) { onFilter(TaskFilter.HIGH) }
            TottFilterChip("Done", filter == TaskFilter.DONE) { onFilter(TaskFilter.DONE) }
        }

        if (visibleTasks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Nothing here", style = MaterialTheme.typography.titleMedium)
                Text("Try another filter or add a task.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val groups = listOf("Today", "Tomorrow", "Later", "Completed")
            LazyColumn(contentPadding = PaddingValues(bottom = 112.dp)) {
                groups.forEach { g ->
                    val items = visibleTasks.filter { TaskRepository.groupOf(it) == g }
                    if (items.isNotEmpty()) {
                        item(key = "h-$g") { GroupHeader(g) }
                        items(items, key = { it.id }) { t ->
                            TaskRow(t, onToggle = { onToggle(t.id) }, onOpen = { onOpenDetail(t.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRow(t: TaskEntity, onToggle: () -> Unit, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // circle checkbox 1:1 (.chk)
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (t.done) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (t.done) {
                Icon(TottIcons2.Check, contentDescription = "Mark ${t.title} complete", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (t.done) TextDecoration.LineThrough else null,
                color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            val n = t.subs.size
            val d = t.subs.count { it.d }
            Text(
                "${t.project} · ${t.due}" + if (n > 0) " · $d/$n subtasks" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (n > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { d.toFloat() / n.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        PriorityPill(t.pri)
    }
}
