package com.tott.app.data

import com.tott.app.data.TaskEntity_
import io.objectbox.Box
import io.objectbox.BoxStore
import javax.inject.Inject
import javax.inject.Singleton

enum class TaskFilter { ALL, TODAY, HIGH, DONE }
enum class TaskSort { DUE, PRI, AZ }

data class DeletedTask(val task: TaskEntity, val index: Int)

@Singleton
class TaskRepository @Inject constructor(
    private val store: BoxStore
) {
    private fun box(): Box<TaskEntity> = store.boxFor(TaskEntity::class.java)

    private fun effectiveOrder(t: TaskEntity): Long = if (t.sortOrder != 0L) t.sortOrder else t.order

    fun observeAll(): List<TaskEntity> = box().all.sortedBy { effectiveOrder(it) }

    fun all(): List<TaskEntity> = box().all.sortedBy { effectiveOrder(it) }

    /**
     * One-time migration for pre-date-model installs. Idempotent: only touches
     * tasks with unset sortOrder/dueDate. Legacy labels map to real dates.
     */
    fun migrateLegacy() {
        val b = box()
        val today = java.time.LocalDate.now().toEpochDay()
        for (t in b.all) {
            var dirty = false
            if (t.sortOrder == 0L) {
                t.sortOrder = if (t.order != 0L) t.order else System.currentTimeMillis()
                dirty = true
            }
            if (t.done && t.doneDate < 0) {
                t.doneDate = today
                dirty = true
            }
            if (t.dueDate < 0) {
                t.dueDate = when (t.due) {
                    "Today" -> today
                    "Tomorrow" -> today + 1
                    "Fri" -> {
                        val dow = java.time.LocalDate.now().dayOfWeek.value // Mon=1..Sun=7
                        today + ((java.time.DayOfWeek.FRIDAY.value - dow + 7) % 7)
                    }
                    else -> -1
                }
                dirty = true
            }
            if (dirty) b.put(t)
        }
    }

    fun get(id: Long): TaskEntity? = box().get(id)

    fun toggle(id: Long): TaskEntity? {
        val t = box().get(id) ?: return null
        t.done = !t.done
        t.doneDate = if (t.done) java.time.LocalDate.now().toEpochDay() else -1
        box().put(t)
        return t
    }

    /** Spawn the next occurrence of a completed recurring task. Returns new id or -1. */
    fun spawnNext(id: Long): Long {
        val b = box()
        val t = b.get(id) ?: return -1
        if (!t.done || (t.rrule != "daily" && t.rrule != "weekly")) return -1
        if (t.dueDate < 0) return -1
        val next = t.copy(
            id = 0,
            done = false,
            doneDate = -1,
            dueDate = t.dueDate + if (t.rrule == "daily") 1 else 7,
            subs = t.subs.map { it.copy(d = false) },
            order = System.currentTimeMillis(),
            sortOrder = (b.all.maxOfOrNull { if (it.sortOrder != 0L) it.sortOrder else it.order } ?: 0L) + 1
        )
        b.put(next)
        return next.id
    }

    fun add(title: String, project: String, due: String, pri: String): TaskEntity {
        val today = java.time.LocalDate.now().toEpochDay()
        val epoch = when (due) {
            "Today" -> today
            "Tomorrow" -> today + 1
            else -> today + ((java.time.DayOfWeek.FRIDAY.value - java.time.LocalDate.now().dayOfWeek.value + 7) % 7)
        }
        return addFull(title, project, epoch, -1, pri)
    }

    fun addFull(title: String, project: String, dueDate: Long, dueTime: Int, pri: String, rrule: String = ""): TaskEntity {
        val b = box()
        val legacy = if (dueDate < 0) "Later" else displayDue(
            TaskEntity(dueDate = dueDate, dueTime = dueTime)
        ).substringBefore(" ")
        val t = TaskEntity(
            title = title, project = project.ifBlank { "Inbox" }, due = legacy,
            pri = pri, done = false, subs = emptyList(), order = System.currentTimeMillis(),
            dueDate = dueDate, dueTime = dueTime, rrule = rrule
        )
        t.sortOrder = (b.all.maxOfOrNull { if (it.sortOrder != 0L) it.sortOrder else it.order } ?: 0L) + 1
        b.put(t)
        return t
    }

    fun updateTitle(id: Long, title: String) {
        val t = box().get(id) ?: return
        if (title.isBlank()) return
        t.title = title
        box().put(t)
    }

    fun toggleSub(id: Long, index: Int) {
        val t = box().get(id) ?: return
        val m = t.subs.toMutableList()
        if (index in m.indices) {
            m[index] = m[index].copy(d = !m[index].d)
            t.subs = m
            box().put(t)
        }
    }

    fun addSub(id: Long, title: String) {
        val t = box().get(id) ?: return
        if (title.isBlank()) return
        t.subs = t.subs + Subtask(title, false)
        box().put(t)
    }

    fun delete(id: Long): TaskEntity? {
        val t = box().get(id) ?: return null
        box().remove(t)
        return t
    }

    fun restore(task: TaskEntity) {
        box().put(task)
    }

    fun clearCompleted(): List<TaskEntity> {
        val gone = box().query(TaskEntity_.done.equal(true)).build().find()
        box().remove(gone)
        return gone
    }

    fun restoreAll(tasks: List<TaskEntity>) {
        box().put(tasks)
    }

    fun projectNames(): List<String> {
        val all = all().map { it.project }.filter { it != "Inbox" }.distinct()
        return listOf("Inbox") + all
    }

    fun projectColorMap(): Map<String, Long> {
        return try {
            store.boxFor(ProjectStyle::class.java).all.associate { it.project to it.colorArgb }
        } catch (e: Exception) { emptyMap() }
    }

    fun setProjectColor(project: String, argb: Long) {
        val b = store.boxFor(ProjectStyle::class.java)
        val existing = b.query(ProjectStyle_.project.equal(project)).build().findFirst()
        if (existing != null) { existing.colorArgb = argb; b.put(existing) }
        else b.put(ProjectStyle(project = project, colorArgb = argb))
    }

    fun clearProjectColor(project: String) {
        val b = store.boxFor(ProjectStyle::class.java)
        b.query(ProjectStyle_.project.equal(project)).build().findFirst()?.let { b.remove(it) }
    }

    companion object {
        private fun todayEpoch(): Long = java.time.LocalDate.now().toEpochDay()

        fun isDueToday(t: TaskEntity): Boolean =
            if (t.dueDate >= 0) t.dueDate == todayEpoch() else t.due == "Today"

        fun isOverdue(t: TaskEntity): Boolean =
            !t.done && t.dueDate >= 0 && t.dueDate < todayEpoch()

        /** Row/detail due label. Falls back to the legacy string when no real date. */
        fun displayDue(t: TaskEntity): String {
            if (t.dueDate < 0) return t.due
            val off = (t.dueDate - todayEpoch()).toInt()
            val day = when {
                off < 0 -> "Overdue"
                off == 0 -> "Today"
                off == 1 -> "Tomorrow"
                off < 7 -> java.time.LocalDate.ofEpochDay(t.dueDate).dayOfWeek
                    .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                else -> java.time.format.DateTimeFormatter.ofPattern("MMM d")
                    .format(java.time.LocalDate.ofEpochDay(t.dueDate))
            }
            if (t.dueTime < 0) return day
            return "$day ${"%02d:%02d".format(t.dueTime / 60, t.dueTime % 60)}"
        }

        fun groupOf(t: TaskEntity): String = when {
            t.done -> "Completed"
            t.dueDate >= 0 -> {
                val off = (t.dueDate - todayEpoch()).toInt()
                when {
                    off < 0 -> "Overdue"
                    off == 0 -> "Today"
                    off == 1 -> "Tomorrow"
                    else -> "Later"
                }
            }
            t.due == "Today" || t.due == "Tomorrow" -> t.due
            else -> "Later"
        }

        fun applyFilter(
            tasks: List<TaskEntity>,
            q: String,
            proj: String?,
            filter: TaskFilter,
            hideDone: Boolean,
            sort: TaskSort
        ): List<TaskEntity> {
            var l = tasks.filter { it.title.contains(q, ignoreCase = true) && (proj == null || it.project == proj) }
            when (filter) {
                TaskFilter.TODAY -> l = l.filter { isDueToday(it) && !it.done }
                TaskFilter.HIGH -> l = l.filter { it.pri == "high" && !it.done }
                TaskFilter.DONE -> l = l.filter { it.done }
                TaskFilter.ALL -> {}
            }
            if (hideDone && filter != TaskFilter.DONE) l = l.filter { !it.done }
            l = when (sort) {
                TaskSort.PRI -> l.sortedBy { mapOf("high" to 0, "med" to 1, "low" to 2)[it.pri] ?: 3 }
                TaskSort.AZ -> l.sortedBy { it.title.lowercase() }
                TaskSort.DUE -> l
            }
            return l
        }
    }
}
