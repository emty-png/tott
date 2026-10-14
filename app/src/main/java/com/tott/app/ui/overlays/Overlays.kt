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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clip(CircleShape)
            .background(Color.Transparent),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    projects: List<String>,
    initialProject: String?,
    initialDate: Long? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, project: String, dueDate: Long, dueTime: Int, pri: String, rrule: String) -> Unit
) {
    val todayEpoch = remember { java.time.LocalDate.now().toEpochDay() }
    var title by remember { mutableStateOf("") }
    var pri by remember { mutableStateOf("Medium") }
    var repeat by remember { mutableStateOf("Never") }
    var dueDate by remember { mutableStateOf(initialDate ?: todayEpoch) }
    var dueTime by remember { mutableStateOf(-1) }
    var proj by remember { mutableStateOf(initialProject ?: "Inbox") }
    var newProj by remember { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    fun dateLabel(): String {
        if (dueDate < 0) return "Pick date"
        val off = (dueDate - todayEpoch).toInt()
        return when {
            off == 0 -> "Today"
            off == 1 -> "Tomorrow"
            else -> java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d")
                .format(java.time.LocalDate.ofEpochDay(dueDate))
        }
    }

    Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()).imePadding()) {
        Text("New task", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Priority", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        SegmentedRow(listOf("Low", "Medium", "High"), pri) { pri = it }
        Text("Due", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDate = true }) { Text(dateLabel()) }
            OutlinedButton(onClick = { showTime = true }, enabled = dueDate >= 0) {
                Text(if (dueTime < 0) "Add time" else "%02d:%02d".format(dueTime / 60, dueTime % 60))
            }
            if (dueDate >= 0 || dueTime >= 0) {
                TextButton(onClick = { dueDate = -1; dueTime = -1 }) { Text("Clear") }
            }
        }
        Text("Repeat", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        SegmentedRow(listOf("Never", "Daily", "Weekly"), repeat) { repeat = it }
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
                    val rr = mapOf("Never" to "", "Daily" to "daily", "Weekly" to "weekly")[repeat] ?: ""
                    onSave(title.trim(), newProj.trim().ifBlank { proj }, dueDate, dueTime, priKey, rr)
                },
                enabled = title.isNotBlank()
            ) { Text("Save") }
        }
    }

    if (showDate) {
        val dpState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = (if (dueDate >= 0) dueDate else todayEpoch) * 86_400_000
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { dueDate = it / 86_400_000 }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }
        ) { androidx.compose.material3.DatePicker(state = dpState) }
    }
    if (showTime) {
        val tpState = androidx.compose.material3.rememberTimePickerState(
            initialHour = if (dueTime >= 0) dueTime / 60 else 9,
            initialMinute = if (dueTime >= 0) dueTime % 60 else 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Time") },
            text = { androidx.compose.material3.TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    dueTime = tpState.hour * 60 + tpState.minute
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { dueTime = -1; showTime = false }) { Text("Remove") }
                    TextButton(onClick = { showTime = false }) { Text("Cancel") }
                }
            }
        )
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
    Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()).imePadding()) {
        var title by remember(task.id, task.title) { mutableStateOf(task.title) }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; if (it.isNotBlank()) onRename(it) },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth()
        )
        val repeatLabel = when (task.rrule) { "daily" -> " · Repeats daily"; "weekly" -> " · Repeats weekly"; else -> "" }
        Text("${task.project} · Due ${com.tott.app.data.TaskRepository.displayDue(task)}$repeatLabel · ${mapOf("high" to "High", "med" to "Medium", "low" to "Low")[task.pri]} priority",
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onReset) { Text("Reset") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onRunPause) { Text(if (running) "Pause" else "Start") }
        }
    }
}
