# Recipe（レシピ）

## 目的

料理レシピの URL とタイトルを日次ジャーナルの `# Recipe` セクションに追記し、
過去のジャーナルから抽出して日付別の履歴として閲覧・検索できるようにする。
OGP で取得したサムネイルを付与してリッチに表示する。

## 画面の役割

- 入力フォーム: URL → `[FETCH]` で OGP タイトル自動取得 → `[+ADD]` で今日のジャーナルに追記。
- 他画面（例: 共有インテント）から URL を事前入力できる（`setPrefilledUrl`）。
- 日付セクション別のレシピ一覧。OGP のタイトル・画像を遅延取得。
- 検索フォーム: 400ms デバウンスで `SearchRecipesUseCase` を実行。AI が利用可能なら
  それをランキングに使う（`AiRepository.availability`）。

## 構成要素

| レイヤ | ファイル | 役割 |
|---|---|---|
| UI | `ui/screen/recipe/RecipeScreen.kt` | 画面 Composable |
| UI | `ui/screen/recipe/RecipeViewModel.kt` | スキャン・追加・OGP 取得・検索 |
| UI | `ui/screen/recipe/RecipeUiState.kt` | `sections`, `addForm`, `searchQuery`, `searchResults`, `aiAvailability` |
| UI | `ui/component/RecipeItemUiModel.kt` | 1 レシピの UI モデル（OGP 含む） |
| Domain | `domain/usecase/AddRecipeToJournalUseCase.kt` | `# Recipe` への APPEND 追記 |
| Domain | `domain/usecase/ScanRecipesUseCase.kt` | Vault 全ジャーナルからレシピ抽出 |
| Domain | `domain/usecase/SearchRecipesUseCase.kt` | タイトル・URL の検索（AI 利用可能ならそれも） |
| Domain | `domain/usecase/RecipeParser.kt` | Markdown ⇄ レシピ行 |
| Data | `data/repository/RecipeRepository.kt` | 抽出済みレシピのキャッシュ（`sections: Flow`） |
| Data | `data/repository/OgpRepository.kt` | OGP 取得 |
| Data | `data/repository/AiRepository.kt` | AI モデル可用性・ダウンロード |

## データフロー

### 一覧（init → refresh）

1. `RecipeRepository.sections` と OGP マップを `combine` し、`RecipeItemUiModel` に変換。
2. 購読の度に `fetchMissingOgp` を走らせ、URL ごとに未取得の OGP を取りに行く。
3. `refresh()` で `ScanRecipesUseCase` を呼び、Vault 内の `# Recipe` を再スキャン。

### 追加（submitAdd）

1. URL・タイトルのバリデーション。
2. `AddRecipeToJournalUseCase(journalDirUri, today, format, title, url)` を実行。
   `# Recipe` セクションに APPEND で追記する。
3. 成功でフォームをクリアし `refresh()`。

### 検索

- `searchQueryFlow` を `debounce(400)` して `runSearch(query)` を実行。
- `SearchRecipesUseCase(query, items)` でランキングを得て、UiState の URL マップで並べ直す。
- 空クエリ時は検索結果をクリア。

## 実装上の注意

- OGP 取得は失敗しても致命ではない（タイトル取得失敗時はユーザにメッセージを出すのみ）。
- `# Recipe` は `APPEND` 戦略。翌日ジャーナル生成時にはセクションごと削除される
  （[../obsidian-journal.md](../obsidian-journal.md) §Cleanup）。
- スキャンの多重実行は `AtomicBoolean` で抑止。
