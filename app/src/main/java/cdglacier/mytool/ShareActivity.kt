package cdglacier.mytool

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import cdglacier.mytool.screen.ShareScreen
import cdglacier.mytool.screen.ShareUiState
import cdglacier.mytool.ui.theme.MyToolTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: intent.getStringExtra(Intent.EXTRA_TITLE)
        val sharedImageUri = if (intent.action == Intent.ACTION_SEND) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else null
        val isImage = intent.type?.startsWith("image/") == true

        setContent {
            MyToolTheme {
                var uiState by remember {
                    mutableStateOf<ShareUiState>(ShareUiState.Loading)
                }
                val snackbarHostState = remember { SnackbarHostState() }

                // Initialize share data
                LaunchedEffect(Unit) {
                    val journalDirUri = ObsidianPreferences.getJournalDirUriFlow(this@ShareActivity).first()
                    if (journalDirUri == null) {
                        uiState = ShareUiState.NoJournalDir
                        return@LaunchedEffect
                    }

                    val datetime = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

                    when {
                        isImage -> {
                            val title = sharedSubject ?: callingAppLabel() ?: "Shared Image"
                            val content = sharedText ?: sharedImageUri?.toString() ?: "[画像]"
                            uiState = ShareUiState.Preview(title, datetime, content)
                        }
                        sharedText != null && isUrl(sharedText) -> {
                            uiState = ShareUiState.Loading
                            val urlContent = UrlFetcher.fetch(sharedText)
                            val title = urlContent.title
                                ?: sharedSubject
                                ?: callingAppLabel()
                                ?: sharedText
                            val content = buildString {
                                append(sharedText)
                                if (!urlContent.description.isNullOrEmpty()) {
                                    append("\n")
                                    append(urlContent.description)
                                }
                            }
                            uiState = ShareUiState.Preview(title, datetime, content)
                        }
                        sharedText != null -> {
                            val title = sharedSubject ?: callingAppLabel() ?: "Shared Content"
                            uiState = ShareUiState.Preview(title, datetime, sharedText)
                        }
                        else -> {
                            uiState = ShareUiState.Error("共有されたコンテンツを取得できませんでした")
                        }
                    }
                }

                ShareScreen(
                    state = uiState,
                    snackbarHostState = snackbarHostState,
                    onConfirm = {
                        val preview = uiState as? ShareUiState.Preview ?: return@ShareScreen
                        lifecycleScope.launch {
                            uiState = ShareUiState.Writing
                            val journalDirUri = ObsidianPreferences.getJournalDirUriFlow(this@ShareActivity).first()
                            val filenameFormat = ObsidianPreferences.getFilenameFormatFlow(this@ShareActivity).first()

                            if (journalDirUri == null) {
                                uiState = ShareUiState.NoJournalDir
                                return@launch
                            }

                            JournalAppender.append(
                                context = this@ShareActivity,
                                journalDirUri = journalDirUri,
                                filenameFormat = filenameFormat,
                                title = preview.title,
                                datetime = preview.datetime,
                                content = preview.content
                            ).fold(
                                onSuccess = {
                                    snackbarHostState.showSnackbar("Journalに追加しました")
                                    finish()
                                },
                                onFailure = { e ->
                                    uiState = ShareUiState.Error(e.message ?: "不明なエラー")
                                }
                            )
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun isUrl(text: String): Boolean =
        text.trimStart().let { it.startsWith("http://") || it.startsWith("https://") }

    private fun callingAppLabel(): String? {
        val packageName = callingActivity?.packageName ?: return null
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) {
            null
        }
    }
}
