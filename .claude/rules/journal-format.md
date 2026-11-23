# Obsidian Journal フォーマット（実装ルール）

Obsidian 日次ジャーナル（`YYYY-MM-DD.md`）に書き込む際に **MyTool 実装が守るべきルール** を定める。

人間向けの全体像・各セクションの用途説明は [`docs/obsidian-journal.md`](../../docs/obsidian-journal.md) を参照。
フォーマットを変更する場合は **docs と本ファイルの両方** を更新し、その後に UseCase / Parser / Writer を実装すること。

---

## 1. 構造の不変条件

- ジャーナルは `---` で囲まれた **管理ブロック** を 1 つ持つ。MyTool の自動書き込みはすべてこの中で行う。
- 管理ブロックの外側（上部・下部フリー領域）は **読まない・書かない・削除しない**。
- 管理ブロックが無い状態で書き込みが発生した場合、`JournalSectionWriter` がファイル末尾に新規作成する。

## 2. セクションの正規順序

書き込み時は必ずこの順序で挿入する。実体は `domain/usecase/JournalSection.kt`（`order` フィールド）。

| # | enum | 見出し | 書き込み戦略 | 主体 UseCase |
|---|---|---|---|---|
| 1 | `TODO` | `# TODO` | CLEANUP_ONLY | （ユーザ手入力 / Widget） |
| 2 | `HABIT_TRACKING` | `# HabitTracking` | CLEANUP_ONLY | `ToggleHabitUseCase` |
| 3 | `RECIPE` | `# Recipe` | APPEND | `AddRecipeToJournalUseCase` |
| 4 | `DIARY` | `# Diary` | APPEND | `AddDiaryToJournalUseCase` |
| 5 | `POSITION_TRACKING` | `# PositionTracking` | REPLACE | `ExportPositionTrackingToJournalUseCase` |
| 6 | `HOUSEHOLD` | `# Household` | REPLACE | `RecordHouseholdEntryUseCase` |
| 7 | `HEALTH` | `# Health` | REPLACE | （予約） |

旧表記（例: `# [[Diary]]`, `# Habit`, `# Position Tracking`）は `legacyHeadings` として読み取り互換を維持する。

## 3. 見出し規約

- H1 (`# `) を使う。前後空白なし、`[[...]]` 不可。
- 見出し行の直後に空行を 1 行入れ、本文はそこから始める。
- セクションの終端判定は「次の `# ` 見出し」または「管理ブロックの閉じ `---`」。

## 4. 書き込みルール

`JournalSectionWriter.write(original, section, body)` を必ず経由する。Writer が保証する不変条件:

1. 管理ブロックが無ければ作る。
2. §2 の順序に反する位置へは挿入しない（既存セクションを読み取り、正しいスロットへ挿入）。
3. 戦略ごとの挙動:
   - **APPEND**: 本文の末尾に新しいブロックを追記する。
   - **REPLACE**: セクション本文を全置換する。
   - **CLEANUP_ONLY**: Writer 経由では生成・削除しない。該当セクションの編集はパーサ経由で
     行単位のみ触る（例: `HabitParser.toggle`）。
4. **冪等性**: 同じ入力で何度呼んでも、結果が 1 回呼んだときと等価。
5. **フリー領域の非破壊**: 管理ブロック外は変更しない。

## 5. Cleanup（翌日生成）

`CopyJournalUseCase` 内で `JournalTransformer.transform()` が管理ブロック内を変換する。

| セクション | 翌日への引き継ぎ |
|---|---|
| `TODO` | `- [x]` 行を削除。残行 0 なら TODO セクションごと削除。 |
| `HABIT_TRACKING` | `- [x]` を `- [ ]` に戻す。 |
| `RECIPE` | セクションごと削除。 |
| `DIARY` | セクションごと削除。 |
| `POSITION_TRACKING` | セクションごと削除。 |
| `HOUSEHOLD` | セクションごと削除。 |
| `HEALTH` | セクションごと削除。 |

管理ブロック外のユーザ記述は変更しない。

## 6. 新セクションを追加する手順

1. [`docs/obsidian-journal.md`](../../docs/obsidian-journal.md) と本ファイル §2/§5 の表にエントリを追加する。
2. `JournalSection` enum に `(heading, legacyHeadings, strategy, order)` を追加する。
3. 対応 UseCase を実装し、`JournalSectionWriter` 経由で書き込む。
4. `JournalTransformer` に cleanup ロジックを追加する。
5. `docs/features/` に機能ドキュメントを追加する。

## 7. 参照ファイル

- Repository: `data/repository/JournalRepository.kt`
- 共通書き込み: `domain/usecase/JournalSectionWriter.kt`
- セクション定義: `domain/usecase/JournalSection.kt`
- Cleanup: `domain/usecase/JournalTransformer.kt`
- 各セクション UseCase: `domain/usecase/Add{Section}ToJournalUseCase.kt` ほか
