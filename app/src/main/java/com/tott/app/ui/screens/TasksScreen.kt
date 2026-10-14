package com.tott.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.tott.app.ui.theme.LocalCompact
import com.tott.app.ui.theme.LocalMotion
import kotlin.math.min

// 1:1 rise motion: rise 350ms decelerate, stagger min(i,10)*35ms
@Composable
private fun RiseIn(index: Int, animKey: String, content: @Composable () -> Unit) {
    if (!LocalMotion.current) { content(); return }
    var visible by remember(animKey) { mutableStateOf(false) }
    LaunchedEffect(animKey) { visible = true }
    val delay = min(index, 10) * 35
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350, delay, FastOutSlowInEasing)) +
            slideInVertically(tween(350, delay, FastOutSlowInEasing)) { it / 2 }
    ) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    allTasks: List<TaskEntity>,
    visibleTasks: List<TaskEntity>,
    query: String,
    searchVisible: Boolean,
    filter: TaskFilter,
    project: String?,
    listState: LazyListState = rememberLazyListState(),
    onQuery: (String) -> Unit,
    onFilter: (TaskFilter) -> Unit,
    onClearProject: () -> Unit,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val animKey = "$filter|$project|$query"
    val searchRequester = remember { FocusRequester() }
    LaunchedEffect(searchVisible) { if (searchVisible) searchRequester.requestFocus() }
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
                    modifier = Modifier.fillMaxWidth().focusRequester(searchRequester)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        val today = allTasks.filter { it.due == "Today" }
        val doneToday = today.count { it.done }
        RiseIn(0, animKey) { HeroCard(open = today.size - doneToday, total = today.size) }
        Spacer(Modifier.height(8.dp))

        if (project != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1:1 project chip carries close icon (HTML data-f="__p")
                FilterChip(
                    selected = true,
                    onClick = onClearProject,
                    label = { Text(project) },
                    leadingIcon = { Icon(TottIcons2.Close, contentDescription = "Clear project", modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(8.dp)
                )
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
            RiseIn(1, animKey) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Nothing here", style = MaterialTheme.typography.titleMedium)
                    Text("Try another filter or add a task.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val groups = listOf("Today", "Tomorrow", "Later", "Completed")
            var flat = 1
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 112.dp)) {
                groups.forEach { g ->
                    val items = visibleTasks.filter { TaskRepository.groupOf(it) == g }
                    if (items.isNotEmpty()) {
                        val hi = flat; flat++
                        item(key = "h-$g") {
                            RiseIn(hi, animKey) { GroupHeader(g) }
                        }
                        items(items, key = { it.id }) { t ->
                            val idx = flat; flat++
                            RiseIn(idx, animKey) {
                                SwipeTaskRow(
                                    t,
                                    onToggle = { onToggle(t.id) },
                                    onDelete = { onDelete(t.id) },
                                    onOpen = { onOpenDetail(t.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeTaskRow(t: TaskEntity, onToggle: () -> Unit, onDelete: () -> Unit, onOpen: () -> Unit) {
    // 1:1 swipe: right (>96px) = toggle, left = delete; bg primary/error-container
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            when (v) {
                SwipeToDismissBoxValue.StartToEnd -> { onToggle(); false }
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
                else -> false
            }
        }
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val target = state.targetValue
            val color = when (target) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            Box(
                Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
            )
        }
    ) {
        TaskRow(t, onToggle = onToggle, onOpen = onOpen)
    }
}

@Composable
fun TaskRow(t: TaskEntity, onToggle: () -> Unit, onOpen: () -> Unit) {
    // 1:1 pop 400ms on check
    val motion = LocalMotion.current
    val compact = LocalCompact.current
    val pop = remember(t.id) { Animatable(1f) }
    LaunchedEffect(t.done, motion) {
        if (t.done && motion) {
            pop.animateTo(1.3f, tween(160, easing = FastOutSlowInEasing))
            pop.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpen)
            .padding(vertical = if (compact) 8.dp else 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // circle checkbox 1:1 (.chk 24px, 2px outline ring)
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(24.dp)
                .graphicsLayer(scaleX = pop.value, scaleY = pop.value)
                .border(2.dp, if (t.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                .background(if (t.done) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
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
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        PriorityPill(t.pri)
    }
}
