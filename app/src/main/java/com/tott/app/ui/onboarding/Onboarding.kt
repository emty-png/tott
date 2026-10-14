package com.tott.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tott.app.ui.icons.TottIcons2
import kotlinx.coroutines.launch

private data class Page(val title: String, val body: String, val icon: @Composable () -> Unit)

@Composable
fun Onboarding(onDone: () -> Unit) {
    val pages = listOf(
        Triple("Capture everything", "Quick-add tasks with priority, due date and subtasks.", @Composable { Icon(TottIcons2.Add, null, Modifier.size(48.dp), MaterialTheme.colorScheme.onPrimaryContainer) }),
        Triple("Organize by project", "Group work, filter, and track progress per project.", @Composable { Icon(TottIcons2.Folder, null, Modifier.size(48.dp), MaterialTheme.colorScheme.onPrimaryContainer) }),
        Triple("Stay in flow", "Insights plus a 25-minute focus timer for deep work.", @Composable { Icon(TottIcons2.Chart, null, Modifier.size(48.dp), MaterialTheme.colorScheme.onPrimaryContainer) })
    )
    val state = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (state.currentPage < pages.size - 1) {
                TextButton(onClick = onDone) { Text("Skip") }
            }
        }
        HorizontalPager(state = state, modifier = Modifier.weight(1f)) { i ->
            val (t, b, ic) = pages[i]
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) { ic() }
                Spacer(Modifier.height(32.dp))
                Text(t, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(b, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 16.dp)) {
            repeat(pages.size) { i ->
                Box(
                    Modifier
                        .size(if (i == state.currentPage) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (i == state.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Button(
            onClick = {
                if (state.currentPage == pages.size - 1) onDone()
                else scope.launch { state.animateScrollToPage(state.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text(if (state.currentPage == pages.size - 1) "Get started" else "Next") }
    }
}
