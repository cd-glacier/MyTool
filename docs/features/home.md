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
| Domain | `domain/usecase/SyncDailySummariesUseCase.kt` | 日次サマリを同期しキャッシュへ |
| Domain | `domain/usecase/GetActivityRatesUseCase.kt` | サマリから UI 用 `DailyActivity` へ変換 |
| Data | `data/repository/DailySummaryRepository.kt` | 日次サマリの永続化・Flow 公開 |

## データフロー

1. `init` で `DailySummaryRepository.summaries` を購読し、`GetActivityRatesUseCase` を通して UiState を更新。
2. `refresh()`（復帰時）で今日から過去 400 日分を `SyncDailySummariesUseCase` に再同期させる。
3. `onSelectDate` で選択日を変更。未来日は無視する。

## 実装上の注意

- 初回ロード時のみ `isLoading = true` を立てる。以降の再同期は裏で静かに実行する。
- サマリの中身（習慣達成率など）は `DailySummaryRepository` 側の責務で、ホームは表示に徹する。
