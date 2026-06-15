package cdglacier.mytool.widget

import cdglacier.mytool.data.repository.GoogleCalendarRepository
import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.data.repository.WidgetConfigRepository
import cdglacier.mytool.domain.usecase.CheckJournalTargetUseCase
import cdglacier.mytool.domain.usecase.CopyJournalUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun journalRepository(): JournalRepository
    fun widgetConfigRepository(): WidgetConfigRepository
    fun googleCalendarRepository(): GoogleCalendarRepository
    fun copyJournalUseCase(): CopyJournalUseCase
    fun checkJournalTargetUseCase(): CheckJournalTargetUseCase
    fun obsidianRepository(): ObsidianRepository
}
