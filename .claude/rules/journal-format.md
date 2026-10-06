# Obsidian Journal フォーマット設計書

このドキュメントは、MyTool が Obsidian の日次ジャーナル（`YYYY-MM-DD.md`）に書き込む
各セクションの **並び順・見出し表記・区切り方・編集方針** を定義する唯一の正本である。

新しいセクションを追加する／既存セクションのフォーマットを変更する場合は、
**先にこのファイルを編集してから** UseCase / Parser / Transformer を実装すること。

---

## 1. 全体レイアウト

ジャーナルは以下の 3 ブロックで構成される。

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

---

{ユーザの任意の記述（下部フリー領域）}
```

- 中央の `---` ... `---` で囲まれた領域を **管理ブロック (managed block)** と呼ぶ。
  MyTool が自動生成・更新するセクションはすべてこの中に収める。
- `---` の外側（上部フリー領域・下部フリー領域）はユーザの手書き領域であり、
  MyTool は読まない・書かない・削除しない。
- 管理ブロックが存在しない場合、各 UseCase はファイル末尾に `---` と自セクションを
  追加し、必要に応じて閉じ `---` も付与する（詳細は §4）。

## 2. セクションの正規順序 (Canonical Order)

管理ブロック内のセクションは、常に以下の順序で並ぶ。
UseCase がセクションを挿入する際は、この順序に従って適切な位置に挿入する。

| # | セクションID | 見出し表記 | 書き込み主体 |
|---|---|---|---|
| 1 | `todo` | `# TODO` | ユーザ手入力（MyTool は cleanup のみ） |
| 2 | `habit_tracking` | `# HabitTracking` | `ToggleHabitUseCase` |
| 3 | `recipe` | `# Recipe` | `AddRecipeToJournalUseCase` |
| 4 | `diary` | `# Diary` | `AddDiaryToJournalUseCase` |
| 5 | `position_tracking` | `# PositionTracking` | `ExportPositionTrackingToJournalUseCase` |
| 6 | `household` | `# Household` | `RecordHouseholdEntryUseCase` |
| 7 | `health` | `# Health` | （将来の HealthConnect 連携用に予約） |

## 3. セクション見出し規約

- セクション見出しはすべて **H1 (`# `)** とする。H2 以降は各セクション内部の構造に使う。
- 見出し文字列は §2 の表に完全一致で記す（前後空白なし、`[[...]]` 不可）。
- 見出し行の直後に空行を 1 行入れ、セクション本文はそこから始める。
- セクションの終端は「次の `# ` 見出し」または「管理ブロックの閉じ `---`」で決まる。

## 4. 挿入 / 更新 / 削除のルール

共通ユーティリティ `JournalSectionWriter`（新設予定）を各 UseCase から利用する。
`JournalSectionWriter` が担保する不変条件：

1. **ブロック存在保証**: 管理ブロックが無ければ、ファイル末尾に `\n---\n...\n---\n` を
   作って挿入する。
2. **順序保証**: §2 の順序に反する位置へは挿入しない。既存セクションを読み取り、
   自セクションの正しいスロットに挿入する。
3. **更新戦略**: 各セクションは「追記型」か「置換型」のいずれかを宣言する。
   - 追記型 (APPEND): `recipe`, `diary`（本文に行を追加）
   - 置換型 (REPLACE): `position_tracking`, `household`, `health`
     （セクション本文を全置換して再生成）
   - cleanup のみ (CLEANUP_ONLY): `todo`, `habit_tracking`
     （MyTool は Writer 経由で書き込まず、ユーザが手で記述する／`toggle` で部分編集するのみ）
4. **冪等性**: 同じ入力で複数回呼ばれても、ジャーナルの中身は 1 回呼んだときと等価に
   なること。
5. **フリー領域の非破壊**: 管理ブロックの外側は一切変更しない。

## 5. Cleanup（`JournalTransformer`）の方針

`JournalTransformer.transform()` は、翌日分のジャーナルを生成する際に呼ばれる。
処理対象は **管理ブロックの中のみ**。各セクションの扱いは以下。

| セクション | 翌日への引き継ぎ処理 |
|---|---|
| `todo` | `- [x]` 済み行を削除。残りはそのまま。残行が 0 なら TODO セクションごと削除。 |
| `habit_tracking` | `- [x]` を `- [ ]` に戻す（チェックリセット）。 |
| `recipe` | セクションごと削除（翌日は空から始まる）。 |
| `diary` | セクションごと削除。 |
| `position_tracking` | セクションごと削除。 |
| `household` | セクションごと削除。 |
| `health` | セクションごと削除。 |

管理ブロック外のユーザ記述は変更しない。

## 6. 新セクション追加の手順

1. 本ファイル §2 の表にエントリを追加する（ID、見出し、書き込み主体、順序）。
2. §4 の更新戦略欄にそのセクションの型（追記型 / 置換型）を記す。
3. §5 の cleanup テーブルに翌日処理を記す。
4. `JournalSection` enum（`domain/usecase/JournalSection.kt`、新設予定）に ID を追加。
5. 対応する UseCase を実装し、`JournalSectionWriter` 経由で書き込む。
6. `JournalTransformer` に cleanup ロジックを追加する。

## 7. 参照ファイル

- Repository: `data/repository/JournalRepository.kt`（ファイル I/O）
- 共通書き込み: `domain/usecase/JournalSectionWriter.kt`（**新設予定**）
- セクション定義: `domain/usecase/JournalSection.kt`（**新設予定**）
- Cleanup: `domain/usecase/JournalTransformer.kt`
- 各セクションの UseCase: `domain/usecase/Add{Section}ToJournalUseCase.kt` ほか
