package cdglacier.mytool.di

import android.content.Context
import androidx.room.Room
import cdglacier.mytool.data.db.LocationRecordDao
import cdglacier.mytool.data.db.MyToolDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MyToolDatabase =
        Room.databaseBuilder(context, MyToolDatabase::class.java, "mytool.db").build()

    @Provides
    fun provideLocationRecordDao(db: MyToolDatabase): LocationRecordDao = db.locationRecordDao()
}
