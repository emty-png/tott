package com.tott.app.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Unique

@Entity
data class ProjectStyle(
    @Id var id: Long = 0,
    @Unique var project: String = "",
    var colorArgb: Long = 0
)
