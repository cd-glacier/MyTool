package cdglacier.mytool.ui.screen.qrstocker

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.QrStockerRepository
import cdglacier.mytool.domain.model.QrEcLevel
import cdglacier.mytool.domain.model.QrEntry
import cdglacier.mytool.domain.usecase.DecodeQrImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class QrStockerViewModel @Inject constructor(
    private val qrStockerRepository: QrStockerRepository,
    private val decodeQrImageUseCase: DecodeQrImageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrStockerUiState())
    val uiState: StateFlow<QrStockerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            qrStockerRepository.observeEntries().collect { entries ->
                _uiState.update { it.copy(entries = entries) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val entries = runCatching { qrStockerRepository.load() }.getOrDefault(emptyList())
            _uiState.update { it.copy(isLoading = false, entries = entries) }
        }
    }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = decodeQrImageUseCase(uri)
            result.fold(
                onSuccess = { decoded ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingDecoded = PendingDecodedUiModel(
                                content = decoded.content,
                                ecLevel = decoded.ecLevel.name,
                                mode = decoded.mode,
                            ),
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "QR を読み取れませんでした")
                    }
                },
            )
        }
    }

    fun onTitleInputChange(title: String) {
        _uiState.update { s ->
            s.copy(pendingDecoded = s.pendingDecoded?.copy(titleInput = title))
        }
    }

    fun onCancelPending() {
        _uiState.update { it.copy(pendingDecoded = null, error = null) }
    }

    fun onConfirmPending() {
        val pending = _uiState.value.pendingDecoded ?: return
        val title = pending.titleInput.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(error = "タイトルを入力してください") }
            return
        }
        viewModelScope.launch {
            _uiState.update { s ->
                s.copy(pendingDecoded = s.pendingDecoded?.copy(isSubmitting = true), error = null)
            }
            val entry = QrEntry(
                title = title,
                content = pending.content,
                ecLevel = QrEcLevel.valueOf(pending.ecLevel),
                mode = pending.mode,
                version = null,
                createdAt = LocalDateTime.now(),
            )
            val result = qrStockerRepository.upsert(entry)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(pendingDecoded = null) }
                    refresh()
                },
                onFailure = { e ->
                    _uiState.update { s ->
                        s.copy(
                            pendingDecoded = s.pendingDecoded?.copy(isSubmitting = false),
                            error = e.message ?: "保存に失敗しました",
                        )
                    }
                },
            )
        }
    }
}
