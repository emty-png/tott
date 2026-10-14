package com.tott.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tott.app.data.TaskFilter
import com.tott.app.data.TaskSort
import com.tott.app.ui.icons.TottIcons2
import com.tott.app.ui.overlays.AddTaskDialog
import com.tott.app.ui.overlays.DetailSheet
import com.tott.app.ui.overlays.FocusSheet
import com.tott.app.ui.screens.InsightsScreen
import com.tott.app.ui.screens.ProjectsScreen
import com.tott.app.ui.screens.TasksScreen
import com.tott.app.ui.onboarding.Onboarding
import com.tott.app.ui.settings.SettingsScreen
import com.tott.app.ui.state.SettingsViewModel
import com.tott.app.ui.state.TottView
import com.tott.app.ui.state.TasksViewModel
import com.tott.app.ui.theme.TottTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TottRoot() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TottRoot(vm: TasksViewModel = hiltViewModel(), settings: SettingsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val firstRun by settings.prefs.firstRun.collectAsState()
    if (firstRun) {
        TottTheme(darkTheme = isSystemInDarkTheme()) {
            Onboarding(onDone = { settings.completeOnboarding() })
        }
        return
    }
    val systemDark = isSystemInDarkTheme()
    val mode by settings.prefs.themeMode.collectAsState()
    val amoled by settings.prefs.amoled.collectAsState()
    val dynamic by settings.prefs.dynamic.collectAsState()
    val accent by settings.prefs.accent.collectAsState()
    val fontScale by settings.prefs.fontScale.collectAsState()
    val compact by settings.prefs.compact.collectAsState()
    val reduceMotion by settings.prefs.reduceMotion.collectAsState()
    val dark = when (mode) { "light" -> false; "dark" -> true; else -> systemDark }
    var showSettings by remember { mutableStateOf(false) }
    BackHandler(enabled = showSettings) { showSettings = false }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    // 1:1 scroll behaviors: hoisted Tasks list state drives FAB collapse + TopBar tint
    val tasksListState = rememberLazyListState()
    var fabExpanded by remember { mutableStateOf(true) }
    var lastScroll by remember { mutableIntStateOf(0) }
    LaunchedEffect(tasksListState) {
        snapshotFlow { tasksListState.firstVisibleItemIndex * 100000 + tasksListState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .map { it }
            .collectLatest { cur ->
                // HTML: fab.min when scrolling down && offset>40; topbar.is-scrolled when >4
                fabExpanded = !(cur > lastScroll && cur > 40)
                lastScroll = cur
            }
    }
    val topScrolled by remember {
        derivedStateOf {
            tasksListState.firstVisibleItemIndex > 0 || tasksListState.firstVisibleItemScrollOffset > 4
        }
    }

    suspend fun showSnack(msg: String, undo: (() -> Unit)?) {
        val res = snack.showSnackbar(msg, actionLabel = if (undo != null) "Undo" else null, duration = SnackbarDuration.Short)
        if (res == SnackbarResult.ActionPerformed) undo?.invoke()
    }

    TottTheme(
        darkTheme = dark,
        dynamicColor = dynamic,
        accentSeed = if (accent == 0xFF6750A4L) null else Color(accent),
        amoled = amoled,
        fontScale = fontScale,
        compact = compact,
        motion = !reduceMotion
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(12.dp)
                ) {
                    Text("TaskFlow", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                    DrawerItem("Tasks", TottIcons2.Task, ui.view == TottView.TASKS && !showSettings) { vm.setView(TottView.TASKS); showSettings = false; scope.launch { drawerState.close() } }
                    DrawerItem("Projects", TottIcons2.Folder, ui.view == TottView.PROJECTS && !showSettings) { vm.setView(TottView.PROJECTS); showSettings = false; scope.launch { drawerState.close() } }
                    DrawerItem("Insights", TottIcons2.Chart, ui.view == TottView.INSIGHTS && !showSettings) { vm.setView(TottView.INSIGHTS); showSettings = false; scope.launch { drawerState.close() } }
                    DrawerItem("Settings", TottIcons2.Settings, showSettings) { showSettings = true; scope.launch { drawerState.close() } }
                    HorizontalDivider(Modifier.padding(16.dp))
                    DrawerItem("Toggle dark theme", TottIcons2.Moon, false) {
                        settings.prefs.setThemeMode(if (dark) "light" else "dark"); scope.launch { drawerState.close() }
                    }
                    DrawerItem("Clear completed (${ui.tasks.count { it.done }})", TottIcons2.Check, false) {
                        scope.launch { drawerState.close() }
                        val msg = vm.clearCompleted()
                        scope.launch { showSnack(msg) { vm.undoClear() } }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                if (showSettings) "Settings" else when (ui.view) {
                                    TottView.TASKS -> "Tasks"
                                    TottView.PROJECTS -> "Projects"
                                    TottView.INSIGHTS -> "Insights"
                                }
                            )
                        },
                        navigationIcon = {
                            if (showSettings) {
                                IconButton(onClick = { showSettings = false }) {
                                    Icon(TottIcons2.Back, contentDescription = "Back")
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(TottIcons2.Menu, contentDescription = "Menu")
                                }
                            }
                        },
                        actions = {
                            if (!showSettings && ui.view == TottView.TASKS) {
                                IconButton(onClick = { vm.toggleSearch() }) {
                                    Icon(TottIcons2.Search, contentDescription = "Search")
                                }
                                Box {
                                    IconButton(onClick = { vm.openMenu() }) {
                                        Icon(TottIcons2.More, contentDescription = "More options")
                                    }
                                    DropdownMenu(expanded = ui.showMenu, onDismissRequest = { vm.closeMenu() }) {
                                        DropCheck("Default order", ui.sort == TaskSort.DUE) { vm.setSort(TaskSort.DUE) }
                                        DropCheck("Sort by priority", ui.sort == TaskSort.PRI) { vm.setSort(TaskSort.PRI) }
                                        DropCheck("Sort A–Z", ui.sort == TaskSort.AZ) { vm.setSort(TaskSort.AZ) }
                                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                        DropCheck("Hide completed", ui.hideDone) { vm.toggleHideDone() }
                                        DropdownMenuItem(text = { Text("Toggle dark theme") }, leadingIcon = { Icon(TottIcons2.Moon, null) }, onClick = { settings.prefs.setThemeMode(if (dark) "light" else "dark"); vm.closeMenu() })
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (topScrolled && ui.view == TottView.TASKS) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (!showSettings) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        NavigationBarItem(
                            selected = ui.view == TottView.TASKS,
                            onClick = { showSettings = false; vm.setView(TottView.TASKS) },
                            icon = {
                                BadgedBox(badge = {
                                    val o = vm.openCount()
                                    if (o > 0) Badge { Text("$o") }
                                }) { Icon(TottIcons2.Task, contentDescription = null) }
                            },
                            label = { Text("Tasks") }
                        )
                        NavigationBarItem(
                            selected = ui.view == TottView.PROJECTS,
                            onClick = { showSettings = false; vm.setView(TottView.PROJECTS) },
                            icon = { Icon(TottIcons2.Folder, contentDescription = null) },
                            label = { Text("Projects") }
                        )
                        NavigationBarItem(
                            selected = ui.view == TottView.INSIGHTS,
                            onClick = { showSettings = false; vm.setView(TottView.INSIGHTS) },
                            icon = { Icon(TottIcons2.Chart, contentDescription = null) },
                            label = { Text("Insights") }
                        )
                    }
                    }
                },
                floatingActionButton = {
                    if (!showSettings && ui.view == TottView.TASKS) {
                        ExtendedFloatingActionButton(
                            text = { Text("New task") },
                            icon = { Icon(TottIcons2.Add, contentDescription = null) },
                            onClick = { vm.openAdd() },
                            expanded = fabExpanded,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snack) }
            ) { pad ->
                Box(Modifier.padding(pad)) {
                    if (showSettings) {
                        SettingsScreen(
                            prefs = settings.prefs,
                            projectNames = ui.tasks.map { it.project }.distinct(),
                            projectColors = vm.projectColors(),
                            onProjectColor = { p, argb -> vm.setProjectColor(p, argb) }
                        )
                    } else when (ui.view) {
                        TottView.TASKS -> TasksScreen(
                            allTasks = ui.tasks,
                            visibleTasks = vm.filtered(),
                            query = ui.query,
                            searchVisible = ui.searchVisible,
                            filter = ui.filter,
                            project = ui.project,
                            listState = tasksListState,
                            onQuery = vm::setQuery,
                            onFilter = vm::setFilter,
                            onClearProject = vm::clearProject,
                            onToggle = { id ->
                                val (msg, canUndo) = vm.toggle(id)
                                scope.launch { showSnack(msg, if (canUndo) vm::undoToggle else null) }
                            },
                            onDelete = { id ->
                                val msg = vm.delete(id)
                                scope.launch { showSnack(msg, vm::undoDelete) }
                            },
                            onOpenDetail = vm::openDetail
                        )
                        TottView.PROJECTS -> ProjectsScreen(ui.tasks, vm.projectColors()) { vm.setProject(it) }
                        TottView.INSIGHTS -> InsightsScreen(ui.tasks)
                    }
                }
            }

            // Add dialog
            if (ui.showAdd) {
                AlertDialog(
                    onDismissRequest = vm::closeAdd,
                    confirmButton = {},
                    text = {
                        AddTaskDialog(
                            projects = listOf("Inbox") + ui.tasks.map { it.project }.filter { it != "Inbox" }.distinct(),
                            initialProject = ui.project,
                            onDismiss = vm::closeAdd,
                            onSave = { title, proj, due, pri ->
                                vm.addTask(title, proj, due, pri)
                                vm.closeAdd()
                                scope.launch { showSnack("Task added", null) }
                            }
                        )
                    }
                )
            }

            // Detail sheet
            val detail = ui.tasks.firstOrNull { it.id == ui.detailId }
            if (detail != null) {
                ModalBottomSheet(onDismissRequest = vm::closeDetail, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                    DetailSheet(
                        task = detail,
                        onRename = { vm.renameTask(detail.id, it) },
                        onToggleSub = { vm.toggleSub(detail.id, it) },
                        onAddSub = { vm.addSub(detail.id, it) },
                        onDelete = {
                            vm.closeDetail()
                            val msg = vm.delete(detail.id)
                            scope.launch { showSnack(msg, vm::undoDelete) }
                        },
                        onFocus = { vm.openFocus(detail.id) },
                        onToggleDone = {
                            vm.closeDetail()
                            val (msg, canUndo) = vm.toggle(detail.id)
                            scope.launch { showSnack(msg, if (canUndo) vm::undoToggle else null) }
                        }
                    )
                }
            }

            // Focus sheet
            val focus = ui.tasks.firstOrNull { it.id == ui.focusId }
            if (focus != null) {
                var left by remember(focus.id) { mutableIntStateOf(1500) }
                var running by remember(focus.id) { mutableStateOf(false) }
                LaunchedEffect(running) {
                    while (running && left > 0) {
                        delay(1000); left--
                        if (left <= 0) {
                            running = false
                            vm.closeFocus()
                            showSnack("Focus session complete", null)
                        }
                    }
                }
                ModalBottomSheet(onDismissRequest = vm::closeFocus) {
                    FocusSheet(
                        title = focus.title,
                        secondsLeft = left,
                        running = running,
                        onRunPause = { running = !running },
                        onReset = { left = 1500 }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DropCheck(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { if (checked) Icon(TottIcons2.Check, contentDescription = null) else Spacer(Modifier.size(24.dp)) },
        onClick = onClick
    )
}
