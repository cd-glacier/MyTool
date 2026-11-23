# Home（ホーム）

## 目的

アプリの入口となる画面。各機能画面への導線に加え、
過去 400 日分の日々の活動（習慣達成率などのサマリ）をカレンダー状のグラフで俯瞰できるようにする。

## 画面の役割

- 各機能（CopyJournal / Habit / Position / Money / Recipe / Household /
  HealthConnect / QrStocker / Diary / Settings）への遷移ボタン。
- 過去約 400 日の `DailyActivity` をマス目で可視化。日付タップで選択日の詳細を表示。
- 今日の達成率（`todayCompletionRate`）を表示。
- 画面復帰時に `SyncDailySummariesUseCase` でサマリを再取得。

## 構成要素

| レイヤ | ファイル | 役割 |
|---|---|---|
| UI | `ui/screen/home/HomeScreen.kt` | 画面 Composable（グラフ＋機能カード群） |
| UI | `ui/screen/home/HomeViewModel.kt` | サマリ購読・同期・選択日管理 |
| UI | `ui/screen/home/HomeUiState.kt` | `dailyActivities`, `todayCompletionRate`, `selectedDate`, `isLoading` |
| Domain | `domain/model/DailySummary.kt` | `date`, `habitRate`, `distanceMeters`, `householdHusbandPoints` |
| Domain | `domain/usecase/SyncDailySummariesUseCase.kt` | 日次サマリを再計算し Room に upsert |
| Domain | `domain/usecase/GetActivityRatesUseCase.kt` | サマリから `DailyActivity` へ変換・活動度合算 |
| Data | `data/repository/DailySummaryRepository.kt` | Room キャッシュ（`summaries: Flow`） |

## データフロー

1. `init` で `DailySummaryRepository.summaries` を購読し、`GetActivityRatesUseCase` を通して UiState を更新。
2. `refresh()`（復帰時）で今日から過去 400 日分を `SyncDailySummariesUseCase` に同期させる。
3. `onSelectDate` で選択日を変更。未来日は無視する。

### `SyncDailySummariesUseCase` の計算内容

各対象日について、以下を集計して `DailySummary` を作り Room に upsert する。

- **`habitRate`**: その日のジャーナルを `HabitParser` でパースし、曜日が一致する習慣だけを対象に
  `完了数 / 対象数` を算出。ジャーナル無し／対象なしは `null`。
- **`distanceMeters`**: `LocationRecordRepository` の生座標を日付でグループ化し、
  連続点間を `Location.distanceBetween` で合算した距離（m）。
- **`householdHusbandPoints`**: `HouseholdJournalParser` でパースし、`Assignee.HUSBAND` の行を
  `points × count + adjustment` で合算（配点は `GetHouseholdPointsUseCase` から）。

対象日の既定の選び方: Room に未登録の日付のみ。ただし当日は常に再計算（`forceAll` で全件強制）。

### `DailyActivity`（グラフ用）

`GetActivityRatesUseCase` が `activityRate` を合成する:

```
activityRate = habitRate × 1/3 + distanceRatio × 2/3
distanceRatio = min(1, distanceMeters / 400,000m)   // 400km = 大阪〜東京
```

いずれも `0` なら `activityRate = null`（表示上は「活動なし」扱い）。

## 実装上の注意

- 初回ロード時のみ `isLoading = true` を立てる。以降の再同期は裏で静かに実行する。
- Journal ディレクトリ未設定時は同期をスキップ（戻り値 0）。
- 距離は端末標準 `Location.distanceBetween`。精度の低い点のフィルタは入れていない。
