# Obsidian Journal フォーマット

MyTool は Obsidian Vault 内の日次ジャーナル（`YYYY-MM-DD.md` などユーザ指定フォーマット）を
各機能のデータソース兼出力先として扱う。本ドキュメントはジャーナルの全体構造と
MyTool が書き込む領域のルールを示す。

> 実装上の正本は `.claude/rules/journal-format.md`。変更するときはそちらを先に更新する。

## 全体レイアウト

ジャーナルは 3 ブロック構造。

```markdown
{ユーザの任意の記述（上部フリー領域）}

---

# TODO
...
# HabitTracking
...
# Recipe
...
# Diary
...
# PositionTracking
...
# Household
...
# Health
...

---

{ユーザの任意の記述（下部フリー領域）}
```

- 中央の `---` ... `---` で囲まれた領域が **管理ブロック**。MyTool が自動生成・更新するセクションはすべてこの中に置く。
- `---` の外側はユーザの手書き領域。MyTool は読まない・書かない・削除しない。
- 管理ブロックが無いジャーナルに書き込みが発生したときは、`JournalSectionWriter` が末尾に新規作成する。

## セクションの正規順序

| # | ID | 見出し | 書き込み戦略 | 書き込み主体 |
|---|---|---|---|---|
| 1 | `todo` | `# TODO` | CLEANUP_ONLY | ユーザ手入力（Widget から一部編集） |
| 2 | `habit_tracking` | `# HabitTracking` | CLEANUP_ONLY | `ToggleHabitUseCase` |
| 3 | `recipe` | `# Recipe` | APPEND | `AddRecipeToJournalUseCase` |
| 4 | `diary` | `# Diary` | APPEND | `AddDiaryToJournalUseCase` |
| 5 | `position_tracking` | `# PositionTracking` | REPLACE | `ExportPositionTrackingToJournalUseCase` |
| 6 | `household` | `# Household` | REPLACE | `RecordHouseholdEntryUseCase` |
| 7 | `health` | `# Health` | REPLACE | (HealthConnect 連携、将来) |

enum の実体は `domain/usecase/JournalSection.kt`。旧表記（`# [[Diary]]`, `# Habit` など）は
`legacyHeadings` として引き続き読み取れる。

### 書き込み戦略

- **APPEND**: 既存セクションに行を追記する。`recipe`, `diary`。
- **REPLACE**: セクション本文を全置換して再生成する。冪等。`position_tracking`, `household`, `health`。
- **CLEANUP_ONLY**: MyTool は Writer 経由で生成・削除しない。既存の `- [ ]` / `- [x]` の
  トグル編集のみ行う。`todo`, `habit_tracking`。

共通処理は `JournalSectionWriter.write(original, section, body)` に集約されており、
各 UseCase はこれを通してセクションを書き込む（順序・ブロック存在保証・冪等性を担保）。

## Cleanup（翌日生成）

`CopyJournalUseCase` がソース日のジャーナルを読み、`JournalTransformer.transform()` で
管理ブロック内を以下のように変換してターゲット日のファイルを生成する。

| セクション | 翌日への引き継ぎ |
|---|---|
| `todo` | `- [x]` 行を削除。残行 0 なら TODO セクションごと削除。 |
| `habit_tracking` | `- [x]` を `- [ ]` に戻す。 |
| `recipe` | セクションごと削除。 |
| `diary` | セクションごと削除。 |
| `position_tracking` | セクションごと削除。 |
| `household` | セクションごと削除。 |
| `health` | セクションごと削除。 |

管理ブロック外のユーザ記述は変更しない。

## Vault 設定

`ObsidianRepository` が DataStore で以下を保持する（Settings 画面で設定）。

- `vaultUri`: Obsidian Vault ルート（任意）
- `journalDirUri`: 日次ジャーナルが置かれるフォルダ URI
- `filenameFormat`: ファイル名のフォーマット文字列（既定 `yyyy-MM-dd'.md'`）
- `pagesDirUri`: Household/Money/Health などページ型データ用フォルダ URI
- `autoCopyEnabled`: 日付が変わったときに自動で翌日ジャーナルを作るか

## 新セクション追加手順

1. `.claude/rules/journal-format.md` §2/§4/§5 を更新。
2. `JournalSection` enum にエントリ追加。
3. 対応 UseCase を実装し `JournalSectionWriter` 経由で書き込む。
4. `JournalTransformer` に cleanup ロジック追加。
5. `docs/features/` に機能ドキュメントを追加。
