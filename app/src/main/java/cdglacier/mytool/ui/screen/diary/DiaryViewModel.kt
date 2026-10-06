package cdglacier.mytool.ui.screen.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cdglacier.mytool.data.repository.DiaryRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.domain.usecase.AddDiaryToJournalUseCase
import cdglacier.mytool.domain.usecase.ScanDiariesUseCase
import cdglacier.mytool.ui.component.DiaryItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val obsidianRepository: ObsidianRepository,
    private val diaryRepository: DiaryRepository,
    private val scanDiariesUseCase: ScanDiariesUseCase,
    private val addDiaryToJournalUseCase: AddDiaryToJournalUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    private val isScanning = AtomicBoolean(false)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    init {
        viewModelScope.launch {
            diaryRepository.sections.collect { sections ->
                val ui = sections.map { section ->
                    DiarySectionUiModel(
                        date = section.date,
                        items = section.items.map {
                            DiaryItemUiModel(timestamp = it.timestamp, content = it.content)
                        },
                    )
                }
                _uiState.update { it.copy(sections = ui) }
            }
        }
    }

    fun refresh() {
        if (!isScanning.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val uri = obsidianRepository.journalDirUri.first() ?: return@launch
                val format = obsidianRepository.filenameFormat.first()
                scanDiariesUseCase(uri.toString(), format)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                isScanning.set(false)
            }
        }
    }

    fun onAddContentChange(content: String) {
        _uiState.update { it.copy(addForm = it.addForm.copy(content = content, error = null)) }
    }

    fun submitAdd() {
        val form = _uiState.value.addForm
        val content = form.content.trim()
        if (content.isBlank()) {
            _uiState.update { it.copy(addForm = it.addForm.copy(error = "本文を入力してください")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(addForm = it.addForm.copy(isSubmitting = true, error = null)) }
            val uri = obsidianRepository.journalDirUri.first()
            if (uri == null) {
                _uiState.update { it.copy(addForm = it.addForm.copy(isSubmitting = false, error = "Vault未設定")) }
                return@launch
            }
            val format = obsidianRepository.filenameFormat.first()
            val timestamp = LocalTime.now().format(timeFormatter)
            val result = addDiaryToJournalUseCase(
                journalDirUri = uri.toString(),
                date = LocalDate.now(),
                filenameFormat = format,
                timestamp = timestamp,
                content = content,
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(addForm = AddDiaryFormUiModel()) }
                    refresh()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(addForm = it.addForm.copy(isSubmitting = false, error = e.message ?: "追加失敗"))
                    }
                },
            )
        }
    }
}
