package cdglacier.mytool.data.repository

import cdglacier.mytool.domain.model.MoneyBook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface MoneyRepository {
    /** vault と pages_dir の設定変化に応じて再ロードされる MoneyBook の Flow */
    fun observeBook(): Flow<MoneyBook>
    suspend fun load(): MoneyBook
    suspend fun save(book: MoneyBook): Result<Unit>
}

@Singleton
class MoneyRepositoryImpl @Inject constructor(
    private val store: PagesFileStore,
) : MoneyRepository {

    companion object {
        const val MONEY_FILENAME = "Money.md"
    }

    override fun observeBook(): Flow<MoneyBook> = flow {
        store.observeChanges().collect { emit(load()) }
    }

    override suspend fun load(): MoneyBook {
        val text = store.readText(MONEY_FILENAME) ?: return MoneyBook()
        return MoneyMarkdown.parse(text)
    }

    override suspend fun save(book: MoneyBook): Result<Unit> =
        store.writeText(MONEY_FILENAME, MoneyMarkdown.serialize(book))
}
