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

    fun observeAll(): List<TaskEntity> = box().all.sortedBy { it.order }

    fun all(): List<TaskEntity> = box().all.sortedBy { it.order }

    fun get(id: Long): TaskEntity? = box().get(id)

    fun toggle(id: Long): TaskEntity? {
        val t = box().get(id) ?: return null
        t.done = !t.done
        box().put(t)
        return t
    }

    fun add(title: String, project: String, due: String, pri: String): TaskEntity {
        val b = box()
        val maxOrder = (b.all.maxOfOrNull { it.order } ?: 0L) + 1
        // unshift behavior in HTML: newest first → use negative order trick; simpler: order = -maxOrder? keep insertion order reversed in query
        val t = TaskEntity(title = title, project = project.ifBlank { "Inbox" }, due = due, pri = pri, done = false, subs = emptyList(), order = System.currentTimeMillis())
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
        fun groupOf(t: TaskEntity): String = when {
            t.done -> "Completed"
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
                TaskFilter.TODAY -> l = l.filter { it.due == "Today" && !it.done }
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
