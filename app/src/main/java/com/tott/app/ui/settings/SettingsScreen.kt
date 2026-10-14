package com.tott.app.ui.settings

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tott.app.data.PrefsRepository
import com.tott.app.ui.icons.TottIcons2
import com.tott.app.ui.overlays.SegmentedRow
import com.tott.app.ui.theme.AccentPresets

val ProjectSwatches = listOf(
    0xFF6750A4L, 0xFF0061A4L, 0xFF006A60L, 0xFF3F6900L,
    0xFF984061L, 0xFF8C4E00L, 0xFFB3261EL, 0xFF625B71L
)

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
fun SettingsScreen(
    prefs: PrefsRepository,
    projectNames: List<String>,
    projectColors: Map<String, Long>,
    onProjectColor: (String, Long?) -> Unit
) {
    val mode by prefs.themeMode.collectAsState()
    val amoled by prefs.amoled.collectAsState()
    val dynamic by prefs.dynamic.collectAsState()
    val accent by prefs.accent.collectAsState()
    val fontScale by prefs.fontScale.collectAsState()
    val compact by prefs.compact.collectAsState()
    val reduceMotion by prefs.reduceMotion.collectAsState()
    var colorTarget by remember { mutableStateOf<String?>(null) }
    val accentName = AccentPresets.firstOrNull { it.first == accent }?.second ?: "Custom"
    val modeLabel = when (mode) { "light" -> "Light"; "dark" -> "Dark"; else -> "System" }
    val fontLabel = mapOf(0.85f to "S", 1f to "M", 1.15f to "L", 1.3f to "XL")[fontScale] ?: "M"

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SectionCard("Appearance · $modeLabel") {
                Text("Theme", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
                SegmentedRow(
                    listOf("System", "Light", "Dark"),
                    modeLabel
                ) { prefs.setThemeMode(it.lowercase()) }
                SwitchRow("AMOLED black", "Pure black surfaces in dark theme", amoled, prefs::setAmoled)
                SwitchRow(
                    "Dynamic color",
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Match your wallpaper (Android 12+)" else "Requires Android 12+",
                    dynamic, prefs::setDynamic,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                )
                Text("Accent · $accentName", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccentPresets.forEach { (argb, _) ->
                        val c = Color(argb)
                        val sel = accent == argb
                        Box(
                            Modifier.size(44.dp)
                                .border(2.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { prefs.setAccent(argb) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (sel) Icon(TottIcons2.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        item {
            SectionCard("Readability · $fontLabel") {
                Text("Text size", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
                SegmentedRow(listOf("S", "M", "L", "XL"), fontLabel) {
                    prefs.setFontScale(mapOf("S" to 0.85f, "M" to 1f, "L" to 1.15f, "XL" to 1.3f)[it] ?: 1f)
                }
                SwitchRow("Compact layout", "Tighter spacing in lists", compact, prefs::setCompact)
            }
        }
        item {
            SectionCard("Motion") {
                SwitchRow("Reduce animations", "Disables entrance and pop effects", reduceMotion, prefs::setReduceMotion)
            }
        }
        item {
            SectionCard("Project colors") {
                if (projectNames.isEmpty()) {
                    Text("No projects yet — add a task to create one.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    projectNames.forEach { p ->
                        val argb = projectColors[p]
                        Row(
                            Modifier.fillMaxWidth().clickable { colorTarget = p }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(32.dp).clip(CircleShape)
                                    .background(if (argb != null) Color(argb) else MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(p.firstOrNull()?.toString() ?: "?", style = MaterialTheme.typography.titleSmall, color = if (argb != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(p, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(if (argb != null) "Custom" else "Default", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            Text(
                "tott 1.0 — that one tool for tasks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    colorTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { colorTarget = null },
            title = { Text(target) },
            text = {
                Column {
                    Text("Pick a color", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProjectSwatches.take(4).forEach { argb ->
                            SwatchDot(argb, projectColors[target] == argb) { onProjectColor(target, argb); colorTarget = null }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProjectSwatches.drop(4).forEach { argb ->
                            SwatchDot(argb, projectColors[target] == argb) { onProjectColor(target, argb); colorTarget = null }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Row {
                    TextButton(onClick = { onProjectColor(target, null); colorTarget = null }) { Text("Default") }
                    TextButton(onClick = { colorTarget = null }) { Text("Close") }
                }
            }
        )
    }
}

@Composable
private fun SwatchDot(argb: Long, selected: Boolean, onPick: () -> Unit) {
    Box(
        Modifier.size(44.dp)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(Color(argb))
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(TottIcons2.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
