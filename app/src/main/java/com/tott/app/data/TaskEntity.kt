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

@Entity
data class TaskEntity(
    @Id var id: Long = 0,
    var title: String = "",
    var project: String = "Inbox",
    var due: String = "Today", // Today | Tomorrow | Fri (+ future dates)
    var pri: String = "med", // high | med | low
    var done: Boolean = false,
    @Convert(converter = SubtaskListConverter::class, dbType = String::class)
    var subs: List<Subtask> = emptyList(),
    var order: Long = 0
)
