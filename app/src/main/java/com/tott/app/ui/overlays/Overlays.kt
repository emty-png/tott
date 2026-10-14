package com.tott.app.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tott.app.data.TaskEntity
import com.tott.app.ui.components.TottFilterChip
import com.tott.app.ui.icons.TottIcons2

@Composable
fun SegmentedRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.clip(CircleShape).background(Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        options.forEachIndexed { i, o ->
            val sel = o == selected
            val shape = when (i) {
                0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 0.dp, bottomEnd = 0.dp)
                options.lastIndex -> RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                else -> RoundedCornerShape(0.dp)
            }
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (sel) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .clickable { onSelect(o) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(o, color = if (sel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    projects: List<String>,
    initialProject: String?,
    onDismiss: () -> Unit,
    onSave: (title: String, project: String, due: String, pri: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var pri by remember { mutableStateOf("Medium") }
    var due by remember { mutableStateOf("Today") }
    var proj by remember { mutableStateOf(initialProject ?: "Inbox") }
    var newProj by remember { mutableStateOf("") }

    Column(Modifier.padding(24.dp)) {
        Text("New task", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Priority", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        SegmentedRow(listOf("Low", "Medium", "High"), pri) { pri = it }
        Text("Due", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        SegmentedRow(listOf("Today", "Tomorrow", "Fri"), due) { due = it }
        Text("Project", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            projects.forEach { p ->
                TottFilterChip(p, proj == p) { proj = p }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = newProj, onValueChange = { newProj = it }, label = { Text("New project (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val priKey = mapOf("Low" to "low", "Medium" to "med", "High" to "high")[pri] ?: "med"
                    onSave(title.trim(), newProj.trim().ifBlank { proj }, due, priKey)
                },
                enabled = title.isNotBlank()
            ) { Text("Save") }
        }
    }
}

@Composable
fun DetailSheet(
    task: TaskEntity,
    onRename: (String) -> Unit,
    onToggleSub: (Int) -> Unit,
    onAddSub: (String) -> Unit,
    onDelete: () -> Unit,
    onFocus: () -> Unit,
    onToggleDone: () -> Unit
) {
    var newSub by remember(task.id) { mutableStateOf("") }
    Column(Modifier.padding(24.dp)) {
        var title by remember(task.id, task.title) { mutableStateOf(task.title) }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; if (it.isNotBlank()) onRename(it) },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth()
        )
        Text("${task.project} · Due ${task.due} · ${mapOf("high" to "High", "med" to "Medium", "low" to "Low")[task.pri]} priority",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        if (task.subs.isNotEmpty()) {
            Text("Subtasks", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))
            task.subs.forEachIndexed { i, s ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggleSub(i) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(24.dp)
                            .border(2.dp, if (s.d) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                            .background(if (s.d) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                            .clickable { onToggleSub(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (s.d) Icon(TottIcons2.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(s.t, style = MaterialTheme.typography.bodyLarge, textDecoration = if (s.d) TextDecoration.LineThrough else null,
                        color = if (s.d) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                }
            }
        } else {
            Text("No subtasks yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = newSub, onValueChange = { newSub = it }, label = { Text("Add subtask") }, singleLine = true, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { if (newSub.isNotBlank()) { onAddSub(newSub.trim()); newSub = "" } },
                colors = IconButtonDefaults.filledTonalIconButtonColors()
            ) { Icon(TottIcons2.Add, contentDescription = "Add subtask") }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDelete) { Text("Delete") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onFocus) { Text("Focus") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onToggleDone) { Text(if (task.done) "Reopen" else "Mark done") }
        }
    }
}

@Composable
fun FocusSheet(title: String, secondsLeft: Int, running: Boolean, onRunPause: () -> Unit, onReset: () -> Unit) {
    fun fmt(s: Int) = String.format("%02d:%02d", s / 60, s % 60)
    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Focus on", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        Text(fmt(secondsLeft), style = MaterialTheme.typography.displayLarge)
        LinearProgressIndicator(
            progress = { (1500 - secondsLeft).toFloat() / 1500f },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
        )
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onReset) { Text("Reset") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onRunPause) { Text(if (running) "Pause" else "Start") }
        }
    }
}
