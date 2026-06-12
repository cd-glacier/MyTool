package cdglacier.mytool.widget

import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.domain.usecase.CopyJournalUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun journalRepository(): JournalRepository
    fun copyJournalUseCase(): CopyJournalUseCase
}
