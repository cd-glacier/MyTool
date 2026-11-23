# PositionTracking（位置情報トラッキング）

## 目的

バックグラウンドで現在地を定期的に記録し、
日次ジャーナルの `# PositionTracking` セクションに自動で出力する。
「どこに行ったか」を Obsidian 側の日記と同じファイル内に残すことで、
ジャーナルを見返すだけで行動記録も辿れる状態にする。

ローカル DB の記録と、過去ジャーナルから抽出したレコードをマージして地図上に表示する。

## 画面の役割

- トラッキング ON/OFF トグル（位置権限が揃っていないと ON にできない）。
- 日付ナビ。今日を表示している間は 60 秒ごとに「日付が変わったか」を検査し、
  自動で今日に追従する（`isFollowingToday`）。
- その日の位置点リスト／地図表示。
- 日付切替時に自動で `AutoExportPositionTrackingIfNeededUseCase` を呼び、
  必要なら `# PositionTracking` セクションを書き出す（スナックバーで件数通知）。
- 権限状態（fine / background）の表示と要求導線。

## 構成要素

| レイヤ | ファイル | 役割 |
|---|---|---|
| UI | `ui/screen/positiontracking/PositionTrackingScreen.kt` | 画面 Composable |
| UI | `ui/screen/positiontracking/PositionTrackingViewModel.kt` | 権限・トラッキング・日付・ジャーナル出力 |
| UI | `ui/screen/positiontracking/PositionTrackingUiState.kt` | 権限・モード・日付・points |
| UI | `ui/component/LocationPointUiModel.kt` | 1 点の表示モデル |
| Domain | `domain/usecase/ToggleLocationTrackingUseCase.kt` | トラッキング起動／停止 |
| Domain | `domain/usecase/ObserveLocationRecordsByDateUseCase.kt` | DB 側レコードの Flow 購読 |
| Domain | `domain/usecase/ExportPositionTrackingToJournalUseCase.kt` | ジャーナルへの REPLACE 書き込み |
| Domain | `domain/usecase/AutoExportPositionTrackingIfNeededUseCase.kt` | 必要時のみ出力する判定付きラッパ |
| Domain | `domain/usecase/ScanJournalLocationsUseCase.kt` | 過去ジャーナルから位置点を抽出し DB へ |
| Domain | `domain/usecase/PositionTrackingParser.kt` | `# PositionTracking` セクションのパース |
| Data | `data/repository/LocationRecordRepository.kt` | 現地記録の DB |
| Data | `data/repository/JournalLocationRepository.kt` | ジャーナル由来レコードの DB |
| Data | `data/repository/TrackingStateRepository.kt` | トラッキング ON/OFF、モード |
| Data | `data/repository/LocationPermissionRepository.kt` | 位置権限状態 |

## データフロー

### トラッキング

- `ToggleLocationTrackingUseCase(enabled)` が ForegroundService を起動／停止し、
  取得した座標を `LocationRecordRepository` に保存する（詳細はサービス側の実装）。

### 表示（observeRecords）

- `ObserveLocationRecordsByDateUseCase(date)` と `JournalLocationRepository.observeByDate(date)` を
  `combine` して `mergePoints` でマージ。
- DB にある時刻と重複するジャーナル由来点は捨て、タイムスタンプ順に並べる。

### ジャーナル出力（autoExportIfNeeded）

- `AutoExportPositionTrackingIfNeededUseCase` が内部で「既に当日分が出力済みか／
  追加分があるか」を判定し、必要なら `# PositionTracking` セクションを REPLACE で書き直す。
- `isExporting` で多重実行を抑止。結果件数をスナックバー表示。

### 今日追従

- `DATE_CHECK_INTERVAL_MS`（60 秒）ごとに現在日を見て、`isFollowingToday` の間は
  日付が変わったタイミングで自動的に今日の表示に切り替える。

## 実装上の注意

- バックグラウンド位置権限が無いとトラッキングを有効化できない。
- `ScanJournalLocationsUseCase` は起動時に 1 度呼ばれ、過去ジャーナルから点を取り込む。
- `# PositionTracking` セクションは `REPLACE` 戦略。冪等に出力し続けられる。
