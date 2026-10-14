package com.tott.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import com.tott.app.data.TaskEntity

@Composable
private fun CardRise(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val delay = kotlin.math.min(index, 10) * 35
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350, delay, FastOutSlowInEasing)) +
            slideInVertically(tween(350, delay, FastOutSlowInEasing)) { it / 2 }
    ) { content() }
}

@Composable
fun ProjectsScreen(tasks: List<TaskEntity>, onOpenProject: (String) -> Unit) {
    val names = tasks.map { it.project }.distinct()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 112.dp)
    ) {
        items(names, key = { it }) { n ->
            val l = tasks.filter { it.project == n }
            val d = l.count { it.done }
            CardRise(names.indexOf(n)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onOpenProject(n) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(n.firstOrNull()?.toString() ?: "?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.padding(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(n, style = MaterialTheme.typography.titleMedium)
                            Text("${l.size - d} open · $d done", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { if (l.isEmpty()) 0f else d.toFloat() / l.size.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    )
                }
            }
            }
        }
    }
}

@Composable
fun InsightsScreen(tasks: List<TaskEntity>) {
    val d = tasks.count { it.done }
    val n = tasks.size
    val r = if (n == 0) 0 else Math.round(d.toFloat() / n.toFloat() * 100)
    val days = listOf("Today", "Tomorrow", "Fri").map { k -> k to tasks.count { it.due == k && !it.done } }
    val mx = maxOf(1, days.maxOf { it.second })
    val hi = tasks.count { it.pri == "high" && !it.done }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 112.dp)
    ) {
        item {
            CardRise(0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Completed", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$d", style = MaterialTheme.typography.displaySmall)
                            Text(" of $n", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                        }
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Completion", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$r", style = MaterialTheme.typography.displaySmall)
                            Text("%", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                        }
                    }
                }
            }
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            CardRise(1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Open tasks by day", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$hi high-priority ${if (hi == 1) "task needs" else "tasks need"} attention",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        days.forEachIndexed { i, (k, v) ->
                            Column(
                                modifier = Modifier.weight(1f).height(120.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((v.toFloat() / mx.toFloat() * 100).dp.coerceAtLeast(4.dp).coerceAtMost(96.dp))
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                        .background(if (i == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("$k · $v", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            }
        }
    }
}
