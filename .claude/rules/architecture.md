# MyTool アーキテクチャガイド

このプロジェクトはGoogle推奨のクリーンアーキテクチャ（UI層・Domain層・Data層）を採用する。

## ディレクトリ構造

```
app/src/main/java/cdglacier/mytool/
├── MainActivity.kt
├── ui/
│   ├── theme/                    -- テーマ定義（Color, Type, Theme）
│   ├── screen/{feature}/         -- 画面ごとのScreen, ViewModel, UiState
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   ├── HomeViewModel.kt
│   │   │   └── HomeUiState.kt
│   │   └── settings/
│   │       ├── SettingsScreen.kt
│   │       ├── SettingsViewModel.kt
│   │       └── SettingsUiState.kt
│   └── component/                -- 再利用可能なUiComponent + UiModel
│       └── {ComponentName}.kt
├── domain/
│   └── usecase/                  -- ユースケース
├── data/
│   └── repository/               -- Repositoryインターフェースと実装
├── widget/                       -- Glance AppWidget関連（独自のライフサイクル）
└── navigation/                   -- Route定義
```

## UI層

一つの画面を実装するには **Screen**, **ViewModel**, **UiState** の3要素が必要。

### Screen

- `@Composable` 関数。画面全体を表す
- 命名: `{Feature}Screen`（例: `HomeScreen`）
- ロジックを持たない。ViewModelから `UiState` を受け取り、ユーザー操作はコールバックでViewModelに委譲する
- ファイル: `ui/screen/{feature}/{Feature}Screen.kt`

```kotlin
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onItemClick: (String) -> Unit,
) {
    // UIの描画のみ。ロジックは書かない
}
```

### ViewModel

- `androidx.lifecycle.ViewModel` を継承する
- 命名: `{Feature}ViewModel`（例: `HomeViewModel`）
- `StateFlow<{Feature}UiState>` を公開し、Screenに状態を提供する
- UiStateの初期化・更新を行う
- UseCaseを呼び出してビジネスロジックを実行する
- ファイル: `ui/screen/{feature}/{Feature}ViewModel.kt`

```kotlin
class HomeViewModel(
    private val getItemsUseCase: GetItemsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onItemClick(id: String) {
        // ロジックはここに書く
    }
}
```

### UiState

- `data class`。画面全体の状態を表す
- 命名: `{Feature}UiState`（例: `HomeUiState`）
- イミュータブル（全プロパティは `val`）
- 複数の `UiModel` を持つことができる
- ファイル: `ui/screen/{feature}/{Feature}UiState.kt`

```kotlin
data class HomeUiState(
    val isLoading: Boolean = false,
    val items: List<ItemUiModel> = emptyList(),
    val header: HeaderUiModel = HeaderUiModel(),
)
```

### UiComponent

- 再利用可能な `@Composable` 関数
- 一つの `UiModel` を引数として受け取る
- ファイル: `ui/component/{ComponentName}.kt`

```kotlin
@Composable
fun TodoItem(
    uiModel: TodoItemUiModel,
    onCheckedChange: (Boolean) -> Unit,
) {
    // コンポーネントの描画
}
```

### UiModel

- `data class`。UiComponentの状態を表す
- 命名: `{ComponentName}UiModel`（例: `TodoItemUiModel`）
- 複数の `UiModel` をネストできる（UiComponentが複数のUiComponentを組み合わせて構成されることを意味する）
- UiComponentと同じファイルに定義する

```kotlin
data class TodoItemUiModel(
    val title: String,
    val isCompleted: Boolean,
    val tags: List<TagUiModel> = emptyList(),  // ネストされたUiModel
)

data class TagUiModel(
    val name: String,
    val color: Color,
)
```

## Domain層

### UseCase

- 一つのビジネスロジック操作を表すクラス
- 命名: `{Action}{Target}UseCase`（例: `CopyJournalUseCase`, `GetTodoItemsUseCase`）
- `operator fun invoke()` パターンを使用する
- Repositoryインターフェースをコンストラクタで受け取る
- ファイル: `domain/usecase/{UseCaseName}.kt`

```kotlin
class GetTodoItemsUseCase(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(date: LocalDate): List<TodoItem> {
        return journalRepository.getJournalContent(date)
            .let { TodoParser.parse(it) }
    }
}
```

## Data層

### Repository

- インターフェースと実装を分離する
- インターフェース命名: `{Domain}Repository`（例: `ObsidianRepository`）
- 実装命名: `{Domain}RepositoryImpl`（例: `ObsidianRepositoryImpl`）
- DataStore、ファイルアクセス等の外部データソースをラップする
- 監視可能なデータは `Flow` を返し、単発操作は `suspend` 関数にする
- ファイル: `data/repository/{RepositoryName}.kt`

```kotlin
interface ObsidianRepository {
    val vaultUri: Flow<Uri?>
    suspend fun saveVaultUri(uri: Uri)
    suspend fun getJournalContent(date: LocalDate): String
}

class ObsidianRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val context: Context,
) : ObsidianRepository {
    // 実装
}
```

## Widget（Glance）

- Widget関連コードは `widget/` パッケージに配置する
- GlanceはScreen/ViewModelパターンの対象外（独自のライフサイクルを持つ）
- WidgetからはRepositoryを直接利用してよい

## 命名規則

| 種類 | 命名パターン | 例 |
|------|-------------|-----|
| Screen | `{Feature}Screen` | `HomeScreen` |
| ViewModel | `{Feature}ViewModel` | `HomeViewModel` |
| UiState | `{Feature}UiState` | `HomeUiState` |
| UiComponent | `{Name}` | `TodoItem` |
| UiModel | `{Name}UiModel` | `TodoItemUiModel` |
| UseCase | `{Action}{Target}UseCase` | `CopyJournalUseCase` |
| Repository (IF) | `{Domain}Repository` | `ObsidianRepository` |
| Repository (実装) | `{Domain}RepositoryImpl` | `ObsidianRepositoryImpl` |
| Route | `{Feature}Route` | `HomeRoute` |

## 実装上の注意

- ViewModelはAndroid Contextに直接依存しない。データアクセスはRepositoryを経由する
- Screen Composableはstateless。状態はすべてViewModelから受け取る
- UiStateとUiModelはイミュータブル（data class + val）
- UseCaseは単一の `invoke` 関数のみ公開する
- Repository interfaceではFlowとsuspend関数を使い分ける
