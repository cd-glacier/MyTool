# HealthConnect（健康データ連携）

## 目的

Android の HealthConnect から睡眠・歩数などの日次データを取得し、
Obsidian Vault の pages フォルダ配下に Markdown として保存する。
過去データのバックフィルにも対応し、PC からも Obsidian で閲覧できる形で健康記録を蓄積する。

書き込み先はジャーナル本体ではなくページ型ファイル（`HealthPageRepository` 経由）。
将来的に日次ジャーナルの `# Health` セクションへの連携も予約されている
（[../obsidian-journal.md](../obsidian-journal.md) 参照）。

## 画面の役割

- HealthConnect 可用性・パーミッション状態の表示と要求。
- DAY / WEEK 表示モード切替、日付ナビ（WEEK なら 7 日単位）。
- 現在日／現在週のデータ表示（歩数、睡眠、睡眠ステージ、チャート）。
- バックフィル（過去 N 日の一括同期）。
- 画面復帰・日付変更時に `autoSync()` で表示範囲を再取得（既存データは上書きしない）。

## 構成要素

| レイヤ | ファイル | 役割 |
|---|---|---|
| UI | `ui/screen/healthconnect/HealthConnectScreen.kt` | 画面 Composable（チャート・睡眠ステージ表示を含む） |
| UI | `ui/screen/healthconnect/HealthConnectViewModel.kt` | 権限・日付・同期・バックフィル |
| UI | `ui/screen/healthconnect/HealthConnectUiState.kt` | 表示モード・日付・読込状態・バックフィル進捗 |
| UI | `ui/screen/healthconnect/HealthFormat.kt` | 表示用フォーマッタ |
| UI | `ui/screen/healthconnect/chart/`, `sleepstage/` | サブコンポーネント |
| Domain | `domain/usecase/SyncHealthDayUseCase.kt` | 1 日分の HealthConnect → ページ書き込み |
| Data | `data/repository/HealthRepository.kt` | HealthConnect SDK ラップ・権限管理 |
| Data | `data/repository/HealthPageRepository.kt` | ページ型 Markdown（`HealthBook`）の読み書き |
| Data | `data/repository/HealthMarkdown.kt`, `SleepMarkdown.kt` | Markdown ⇄ モデル変換 |
| Data | `data/repository/SleepPageRepository.kt` | 睡眠ページ専用の I/O |

## データフロー

### 同期（autoSync / onDateChange）

1. HealthConnect 可用性・権限・pages ディレクトリ設定を確認。
2. 表示モードに応じて対象日群を決定（DAY: 1 日、WEEK: 月〜日 7 日）。
3. 各対象日について `SyncHealthDayUseCase(date, overwrite = false)` を実行。
4. 内部では HealthConnect から取得 → `HealthMarkdown`/`SleepMarkdown` で整形 → 既存ページに
   マージ or 新規書き込み。

### バックフィル（onBackfill）

- 今日の前日から遡って N 日分を順次同期。進捗を `BackfillProgress(index, total)` で UI へ反映。
- 既存データは温存（`overwrite = false`）。

### 読み込み

- `HealthPageRepository.observeBook()` の `HealthBook` を購読し、
  表示用に `currentDay` / `currentWeek` を都度再計算（`recomputeDerived`）。

## ページ Markdown フォーマット

書き込み先は Vault の pages フォルダ配下、2 ファイル。

### `Health.md`（日次サマリ）

`HealthMarkdown.serialize()` が生成。1 テーブルで 1 行 = 1 日、列 = 指標。
日付は降順で並び、未取得指標は空セル。

```markdown
# Health

## Daily
| Date | Steps | Dist_m | ActMin | ActKcal | TotalKcal | HrAvg | HrMin | HrMax | RestHr | SleepMin | Sys | Dia | Glucose | SpO2 | Temp | RespRate |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-11-09 | 8432 | 6123.45 | 42 | 212.3 | 1987.12 | 78 | 54 | 142 | 58 | 412 |  |  |  | 97.2 |  |  |
| 2026-11-08 | 10210 | 7812 | 51 | 258 | 2040 | 81 | 56 | 150 | 59 | 395 |  |  |  |  |  |  |
```

- 数値は整数なら整数文字列、小数なら `%.2f`。
- パース時はヘッダ名で列マッピングを復元するため、列順入れ替えや既存列の削除があっても耐える。
- `## Daily` セクション以外は無視される。

### `SleepStages.md`（睡眠ステージ詳細）

`SleepMarkdown.serialize()` が生成。1 行 = 1 ステージ。`Date / SessionStart / SessionEnd / Stage / StageStart / StageEnd`。
時刻は ISO-8601 Instant。

```markdown
# Sleep Stages

## Stages
| Date | SessionStart | SessionEnd | Stage | StageStart | StageEnd |
|---|---|---|---|---|---|
| 2026-11-09 | 2026-11-08T23:14:00Z | 2026-11-09T06:48:00Z | deep | 2026-11-08T23:30:00Z | 2026-11-09T00:12:00Z |
| 2026-11-09 | 2026-11-08T23:14:00Z | 2026-11-09T06:48:00Z | light | 2026-11-09T00:12:00Z | 2026-11-09T01:05:00Z |
| 2026-11-09 | 2026-11-08T23:14:00Z | 2026-11-09T06:48:00Z | rem | 2026-11-09T01:05:00Z | 2026-11-09T02:10:00Z |
```

- 読み込み時は `(SessionStart, SessionEnd)` でグルーピングしてセッション単位に復元する。
- `Stage` は `SleepStageType.key`（例: `deep`, `light`, `rem`, `awake` など）。

## 実装上の注意

- HealthConnect が未対応の端末では `healthConnectAvailable = false` として機能を無効化。
- 週計算は ISO 月曜始まり（`DayOfWeek.value` を 0-index 化）。
- 同期中（`isSyncing`）およびバックフィル中は重複実行を抑止する。
