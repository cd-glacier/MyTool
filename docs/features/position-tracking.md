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

## トラッキングモード

| モード | Priority | 取得間隔 | 取得方式 |
|---|---|---|---|
| `STATIONARY` (省電力) | `PRIORITY_BALANCED_POWER_ACCURACY` | 60 秒 | PendingIntent + BroadcastReceiver |
| `MOVING` (高精度) | `PRIORITY_HIGH_ACCURACY` (GPS) | 60 秒 | **ForegroundService** + LocationCallback |

高精度モード時は `LocationTrackingForegroundService` を起動し、`FOREGROUND_SERVICE_LOCATION`
タイプで常駐させる。これにより OS による高精度取得の抑制を避け、安定した 60 秒間隔での
GPS 取得を実現する。省電力モードは従来通り PendingIntent 方式で動かし、常駐通知は出さない。

### 通知

高精度モード常駐通知:

- チャンネル: `location_tracking`（LOW importance、音なし）
- 文言: 「位置情報を記録中（高精度）」
- タップ時: PositionTracking 画面へ遷移（`MainActivity` 経由）

## モード切替

STATIONARY → MOVING（以下いずれかで切替）:

- 直前地点との距離が `MOVE_THRESHOLD_METERS`（100m）を超えた
- ActivityRecognition が `IN_VEHICLE` / `ON_BICYCLE` / `RUNNING` / `WALKING` を高確度で検出した

MOVING → STATIONARY（以下いずれかで切替）:

- 最終移動から `STATIONARY_TIMEOUT_MS`（5 分）以上経過した
- ActivityRecognition が `STILL` を高確度で検出した

### ActivityRecognition

- `ACTIVITY_RECOGNITION` 権限を利用。`ActivityRecognitionClient` へ PendingIntent 登録。
- 購読はトラッキング ON の間のみ。`start()` で登録、`stop()` で解除。
- 既存の距離ベース判定と **OR 条件で共存**（どちらが先に発火してもモード切替する）。
- confidence の閾値は 75 以上を採用。

## データフロー

### トラッキング

- `ToggleLocationTrackingUseCase(enabled)` が `LocationTrackingManager.start/stop` を呼ぶ。
- `start` 時:
  - `FusedLocationProviderClient` に PendingIntent で現在モードの更新をリクエスト
  - MOVING なら `LocationTrackingForegroundService` を起動
  - `ActivityRecognitionClient` に PendingIntent で遷移検知を登録
- 位置更新は `LocationUpdateReceiver`（STATIONARY）または Service 内 `LocationCallback`（MOVING）で受信し `LocationRecordRepository` に保存。
- モード切替は `LocationUpdateReceiver` / `ActivityTransitionReceiver` から `LocationTrackingManager.switchMode()` を呼んで行う。

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
