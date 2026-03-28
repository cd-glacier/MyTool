package cdglacier.mytool.ui.screen.iconpack

import androidx.lifecycle.ViewModel
import cdglacier.mytool.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class IconPackViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        IconPackUiState(
            icons = listOf(
                IconPackEntry(
                    appName = "LINE",
                    packageName = "jp.naver.line.android",
                    drawableRes = R.drawable.ic_pack_line,
                ),
                IconPackEntry(
                    appName = "ClaudeCode",
                    packageName = "com.anthropic.claudecode",
                    drawableRes = R.drawable.ic_pack_claudecode,
                ),
                IconPackEntry(
                    appName = "Arc Browser",
                    packageName = "company.thebrowser.browser",
                    drawableRes = R.drawable.ic_pack_arc,
                ),
                IconPackEntry(
                    appName = "画面ロック",
                    packageName = "(ランチャーショートカット)",
                    drawableRes = R.drawable.ic_pack_screenlock,
                ),
            )
        )
    )
    val uiState: StateFlow<IconPackUiState> = _uiState.asStateFlow()
}
