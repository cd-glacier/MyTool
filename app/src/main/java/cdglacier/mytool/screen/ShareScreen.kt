package cdglacier.mytool.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed interface ShareUiState {
    data object Loading : ShareUiState
    data class Preview(val title: String, val datetime: String, val content: String) : ShareUiState
    data object Writing : ShareUiState
    data object NoJournalDir : ShareUiState
    data class Error(val message: String) : ShareUiState
}

@Composable
fun ShareScreen(
    state: ShareUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is ShareUiState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("URLの情報を取得中...")
                    }
                }

                is ShareUiState.Writing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Journalに追加中...")
                    }
                }

                is ShareUiState.Preview -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Journalに追加",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "# ${state.title} [[Bookmark]]",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = state.datetime,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("キャンセル")
                            }
                            Button(
                                onClick = onConfirm,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("追加")
                            }
                        }
                    }
                }

                is ShareUiState.NoJournalDir -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Journalフォルダが設定されていません",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "MyToolの設定でJournalフォルダを選択してください",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onCancel) {
                            Text("閉じる")
                        }
                    }
                }

                is ShareUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "エラーが発生しました",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onCancel) {
                            Text("閉じる")
                        }
                    }
                }
            }
        }
    }
}
