package com.tott.app.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 1:1 icons from taskflow.html P dict.
 * Where platform Material icon is pixel-identical we alias it;
 * otherwise we build from the exact path data.
 */
private fun fromPath(name: String, data: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        addPathNodes(data)
    }.build()

object TottIcons2 {
    val Menu: ImageVector = Icons.Filled.Menu
    val Search: ImageVector = Icons.Filled.Search
    val Add: ImageVector = Icons.Filled.Add
    val Check: ImageVector = Icons.Filled.Check
    val Close: ImageVector = Icons.Filled.Close
    val Task: ImageVector = Icons.Filled.TaskAlt
    val Folder: ImageVector = Icons.Filled.Folder
    val More: ImageVector = Icons.Filled.MoreVert
    val Moon: ImageVector = Icons.Filled.DarkMode
    val Back: ImageVector = Icons.Filled.ArrowBack
    val Settings: ImageVector = Icons.Filled.Settings
    // Bar chart path is custom in HTML (P_CHART), keep exact:
    val Chart: ImageVector by lazy { fromPath("chart", TottIcons.P_CHART) }
}
