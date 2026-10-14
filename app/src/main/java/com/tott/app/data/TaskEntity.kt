package com.tott.app.data

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.converter.PropertyConverter

data class Subtask(val t: String, val d: Boolean)

class SubtaskListConverter : PropertyConverter<List<Subtask>, String> {
    override fun convertToDatabaseValue(entityProperty: List<Subtask>?): String {
        if (entityProperty.isNullOrEmpty()) return ""
        // t escaped: \ and | and ; are escaped
        return entityProperty.joinToString(";") { s ->
            val e = s.t.replace("\\", "\\\\").replace("|", "\\|").replace(";", "\\;")
            "$e|${if (s.d) 1 else 0}"
        }
    }

    override fun convertToEntityProperty(databaseValue: String?): List<Subtask> {
        if (databaseValue.isNullOrEmpty()) return emptyList()
        val out = mutableListOf<Subtask>()
        var cur = StringBuilder()
        val parts = mutableListOf<String>()
        var esc = false
        for (c in databaseValue) {
            if (esc) { cur.append(c); esc = false }
            else if (c == '\\') esc = true
            else if (c == ';') { parts.add(cur.toString()); cur = StringBuilder() }
            else cur.append(c)
        }
        parts.add(cur.toString())
        for (p in parts) {
            // split on last unescaped | — our escape already resolved, so split last |
            val idx = p.lastIndexOf('|')
            if (idx < 0) continue
            val title = p.substring(0, idx).replace("\\|", "|").replace("\\;", ";").replace("\\\\", "\\")
            val done = p.substring(idx + 1) == "1"
            out.add(Subtask(title, done))
        }
        return out
    }
}

/** Unit-separator-joined string list (URIs never contain a raw US character). */
class StringListConverter : PropertyConverter<List<String>, String> {
    override fun convertToDatabaseValue(entityProperty: List<String>?): String {
        return entityProperty?.joinToString("\u001F") ?: ""
    }

    override fun convertToEntityProperty(databaseValue: String?): List<String> {
        if (databaseValue.isNullOrEmpty()) return emptyList()
        return databaseValue.split("\u001F")
    }
}

@Entity
data class TaskEntity(
    @Id var id: Long = 0,
    var title: String = "",
    var project: String = "Inbox",
    var due: String = "Today", // legacy label; used only when dueDate < 0
    var pri: String = "med", // high | med | low
    var done: Boolean = false,
    @Convert(converter = SubtaskListConverter::class, dbType = String::class)
    var subs: List<Subtask> = emptyList(),
    var order: Long = 0,
    /** Epoch day (LocalDate.toEpochDay). -1 = legacy `due` string. */
    var dueDate: Long = -1,
    /** Minutes since midnight. -1 = no time. */
    var dueTime: Int = -1,
    /** Recurrence: "" | "daily" | "weekly". */
    var rrule: String = "",
    /** Manual position. 0 = not yet migrated (falls back to `order`). */
    var sortOrder: Long = 0,
    @Convert(converter = StringListConverter::class, dbType = String::class)
    var imageUris: List<String> = emptyList()
)
