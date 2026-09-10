package cdglacier.mytool.ui.screen.qrstocker.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.QrStockerRepository
import cdglacier.mytool.domain.usecase.GenerateQrBitmapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrStockerDetailViewModel @Inject constructor(
    private val qrStockerRepository: QrStockerRepository,
    private val generateQrBitmapUseCase: GenerateQrBitmapUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrStockerDetailUiState())
    val uiState: StateFlow<QrStockerDetailUiState> = _uiState.asStateFlow()

    fun load(title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val entry = qrStockerRepository.load().firstOrNull { it.title == title }
            if (entry == null) {
                _uiState.update { it.copy(isLoading = false, error = "エントリが見つかりません") }
                return@launch
            }
            val bitmap = generateQrBitmapUseCase(entry).getOrNull()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    entry = entry,
                    bitmap = bitmap,
                    error = if (bitmap == null) "QR の生成に失敗しました" else null,
                )
            }
        }
    }
}
