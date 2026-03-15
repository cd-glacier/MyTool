package cdglacier.mytool.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private data class NavItem(val label: String, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCopyObsidianJournal: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val items = listOf(
        NavItem(label = "CopyObsidianJournal", onClick = onNavigateToCopyObsidianJournal),
        NavItem(label = "Settings", onClick = onNavigateToSettings)
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                ListItem(
                    headlineContent = { Text(item.label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { item.onClick() }
                )
                HorizontalDivider()
            }
        }
    }
}
