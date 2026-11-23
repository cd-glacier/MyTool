# Household（家事記録）

## 目的

家事ごとに事前設定したポイント（`HouseholdPoint`）を使って、
その日に夫 / 妻がそれぞれ何をどれだけこなしたかを記録し、
日次ジャーナルの `# Household` セクションへポイント集計付きで書き出す。

ポイント定義は [HouseholdPoints](./household-points.md) 画面で管理する。

## 画面の役割

- 日付ナビ。
- その日の集計（夫合計 / 妻合計 / 家事別内訳）を表示。
- `[RECORD]` ダイアログで家事名・担当者・回数・調整値を入力して追加。
- 使用頻度の高いポイント順にソート表示（よく使う家事をすぐ選べるように）。
- 直前に選んだ担当者（`lastAssignee`）を次回ダイアログの既定値にする。

## 構成要素

| レイヤ | ファイル | 役割 |
|---|---|---|
| UI | `ui/screen/household/HouseholdScreen.kt` | 画面 Composable |
| UI | `ui/screen/household/HouseholdViewModel.kt` | 日付操作・ダイアログ・記録・楽観的更新 |
| UI | `ui/screen/household/HouseholdUiState.kt` | `summary`, `points`, `recordDialog`, `lastAssignee` ほか |
| Domain | `domain/model/HouseholdEntry.kt`, `HouseholdPoint.kt`, `HouseholdSummary.kt`, `Assignee.kt` | ドメインモデル |
| Domain | `domain/usecase/RecordHouseholdEntryUseCase.kt` | 入力をジャーナルに追記／再計算 |
| Domain | `domain/usecase/GetHouseholdSummaryUseCase.kt` | 日付範囲のサマリ取得 |
| Domain | `domain/usecase/GetHouseholdPointsUseCase.kt` | ポイント定義の取得 |
| Domain | `domain/usecase/HouseholdJournalParser.kt` | `# Household` セクションのパース |
| Data | `data/repository/HouseholdRepository.kt` | ページ側のポイント定義とジャーナル I/O 連携 |

## データフロー

### 読み込み（refresh）

1. `journalDirUri` と `pagesDirUri` の設定有無を確認。
2. pages 設定済みなら `GetHouseholdPointsUseCase` でポイント定義を取得。
3. journal 設定済みなら `GetHouseholdSummaryUseCase(date..date)` で当日サマリを取得。
4. サマリ内の使用回数でポイント配列を並び替え（`sortPointsByUsage`）。

### 記録（onSubmitRecord）

1. 入力バリデーション（名前空白不可、回数 > 0、調整値はパース可）。
2. `RecordHouseholdEntryUseCase(date, HouseholdEntry)` を呼ぶ。
   内部で `# Household` セクションを REPLACE 戦略で再生成し書き込む。
3. 成功時は楽観的更新（`applyOptimistic`）で UI を即時反映し、その後 `refresh()` で確定。
4. 失敗時は `errorMessage` を立てダイアログを維持。

### ジャーナルフォーマット

`# Household` セクションは `REPLACE` 戦略。詳細な行フォーマットは
`HouseholdJournalParser` / `RecordHouseholdEntryUseCase` の実装を参照。

## 実装上の注意

- ポイントは `HouseholdPoints` 画面側で管理され、ここでは参照のみ。
- 楽観的更新は「ポイント × 回数 + 調整」を直接 summary に加算して行う。
- `Assignee` は現状 `HUSBAND` / `WIFE` 固定。
