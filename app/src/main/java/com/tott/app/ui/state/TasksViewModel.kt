package com.tott.app.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tott.app.data.PrefsRepository
import com.tott.app.data.ReminderScheduler
import com.tott.app.data.TaskEntity
import com.tott.app.data.TaskFilter
import com.tott.app.data.TaskRepository
import com.tott.app.data.TaskSort
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TottView { TASKS, PROJECTS, CALENDAR, INSIGHTS }

data class TottUiState(
    val view: TottView = TottView.TASKS,
    val filter: TaskFilter = TaskFilter.ALL,
    val query: String = "",
    val searchVisible: Boolean = false,
    val project: String? = null,
    val sort: TaskSort = TaskSort.DUE,
    val hideDone: Boolean = false,
    val tasks: List<TaskEntity> = emptyList(),
    val darkTheme: Boolean? = null, // null = follow system
    val detailId: Long? = null,
    val showAdd: Boolean = false,
    val addDate: Long? = null,
    val showMenu: Boolean = false,
    val showDrawer: Boolean = false,
    val focusId: Long? = null
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repo: TaskRepository,
    private val reminders: ReminderScheduler
) : ViewModel() {
    private val _ui = MutableStateFlow(TottUiState())
    val ui: StateFlow<TottUiState> = _ui.asStateFlow()

    // undo buffers (mirrors HTML snack undo)
    private var lastDeleted: TaskEntity? = null
    private var lastCleared: List<TaskEntity>? = null
    private var lastToggledToDone: Long? = null
    private val migrated = java.util.concurrent.atomic.AtomicBoolean(false)

    init { refresh() }

    // DB I/O off the main thread; lists are small but this keeps scroll/input jank-free.
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            if (migrated.compareAndSet(false, true)) repo.migrateLegacy()
            val all = repo.all()
            _ui.update { it.copy(tasks = all) }
        }
    }

    fun filtered(): List<TaskEntity> {
        val s = _ui.value
        return TaskRepository.applyFilter(s.tasks, s.query, s.project, s.filter, s.hideDone, s.sort)
    }

    fun openCount(): Int = _ui.value.tasks.count { !it.done }

    // nav
    fun setView(v: TottView) = _ui.update { it.copy(view = v, searchVisible = false, query = "", project = null, detailId = null, focusId = null, showAdd = false, showMenu = false, showDrawer = false) }
    fun setFilter(f: TaskFilter) = _ui.update { it.copy(filter = f) }
    fun setQuery(q: String) = _ui.update { it.copy(query = q) }
    fun toggleSearch() = _ui.update { it.copy(searchVisible = !it.searchVisible, query = "") }
    fun setProject(p: String?) = _ui.update { it.copy(project = p, view = TottView.TASKS, filter = TaskFilter.ALL) }
    fun clearProject() = _ui.update { it.copy(project = null) }
    fun setSort(s: TaskSort) = _ui.update { it.copy(sort = s, showMenu = false) }
    fun toggleHideDone() = _ui.update { it.copy(hideDone = !it.hideDone, showMenu = false) }
    fun toggleTheme() = _ui.update { cur ->
        // null(system)-> flip based on nothing; UI layer resolves system and flips
        cur.copy(darkTheme = !(cur.darkTheme ?: false))
    }
    fun setDarkExplicit(dark: Boolean) = _ui.update { it.copy(darkTheme = dark) }

    // overlays
    fun openDetail(id: Long) = _ui.update { it.copy(detailId = id) }
    fun closeDetail() = _ui.update { it.copy(detailId = null) }
    fun openAdd(date: Long? = null) = _ui.update { it.copy(showAdd = true, addDate = date) }
    fun closeAdd() = _ui.update { it.copy(showAdd = false, addDate = null) }
    fun openMenu() = _ui.update { it.copy(showMenu = true) }
    fun closeMenu() = _ui.update { it.copy(showMenu = false) }
    fun openDrawer() = _ui.update { it.copy(showDrawer = true) }
    fun closeDrawer() = _ui.update { it.copy(showDrawer = false) }
    fun openFocus(id: Long) = _ui.update { it.copy(focusId = id, detailId = null) }
    fun closeFocus() = _ui.update { it.copy(focusId = null) }

    // mutations return snackbar message + whether undo available
    private fun syncReminder(id: Long) {
        val t = repo.get(id) ?: return
        if (!t.done && t.dueDate >= 0 && t.dueTime >= 0) reminders.schedule(t)
        else reminders.cancel(id)
    }

    fun toggle(id: Long): Pair<String, Boolean> {
        val t = repo.get(id) ?: return "" to false
        repo.toggle(id)
        // Completing a recurring task spawns its next occurrence (undo reopens; spawned copy stays).
        if (!t.done && t.rrule.isNotEmpty()) repo.spawnNext(id)
        syncReminder(id); refresh()
        return if (!t.done) {
            lastToggledToDone = id
            "Task completed" to true
        } else {
            lastToggledToDone = null
            "Task reopened" to false
        }
    }

    fun undoToggle() {
        lastToggledToDone?.let { repo.toggle(it); syncReminder(it); refresh() }
        lastToggledToDone = null
    }

    fun delete(id: Long): String {
        reminders.cancel(id)
        lastDeleted = repo.delete(id); refresh()
        return "Task deleted"
    }

    fun undoDelete() {
        lastDeleted?.let { repo.restore(it); syncReminder(it.id); refresh() }
        lastDeleted = null
    }

    fun clearCompleted(): String {
        val gone = repo.clearCompleted()
        gone.forEach { reminders.cancel(it.id) }
        refresh()
        lastCleared = gone
        return "${gone.size} completed cleared"
    }

    fun undoClear() {
        lastCleared?.let { repo.restoreAll(it); it.forEach { t -> syncReminder(t.id) }; refresh() }
        lastCleared = null
    }

    fun addTask(title: String, project: String, dueDate: Long, dueTime: Int, pri: String, rrule: String = "") {
        val t = repo.addFull(title, project, dueDate, dueTime, pri, rrule)
        syncReminder(t.id); refresh()
    }

    fun sendTestNotification() = reminders.sendTest()

    fun projectColors(): Map<String, Long> = repo.projectColorMap()

    fun setProjectColor(project: String, argb: Long?) {
        if (argb == null) repo.clearProjectColor(project) else repo.setProjectColor(project, argb)
        refresh()
    }

    fun renameTask(id: Long, title: String) { repo.updateTitle(id, title); refresh() }
    fun toggleSub(id: Long, i: Int) { repo.toggleSub(id, i); refresh() }
    fun addSub(id: Long, t: String) { repo.addSub(id, t); refresh() }
}
