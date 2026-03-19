package cdglacier.mytool.di

import cdglacier.mytool.widget.WidgetPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetPreferences(): WidgetPreferences
}
