# Widgets（ホーム画面ウィジェット）

## 目的

Obsidian ジャーナルと Google カレンダーの情報を、アプリを開かずにホーム画面から
参照・操作できるようにする。Glance ベースの AppWidget として実装している。

> 本プロジェクトでは Widget は Screen/ViewModel パターンの対象外（独自のライフサイクル）とし、
> Repository を直接利用してよい（`.claude/rules/architecture.md`）。

## 提供する Widget

### JournalTodoWidget

今日のジャーナル `# TODO` セクションのチェックリストを表示し、タップでトグルする。

- 設定アクティビティ: `JournalTodoWidgetConfigActivity`（表示件数などを選べる想定）
- 受信: `JournalTodoWidgetReceiver`
- 更新: `JournalTodoWidget` + `WidgetUpdateWorker` / `BaseWidgetUpdateWorker`
- TODO 行のパース: `widget/TodoParser.kt`
- 書き戻し: `JournalRepository` を介して該当行の `[ ]` / `[x]` をトグル
  （`JournalSection.TODO` は `CLEANUP_ONLY` なので、Widget からの編集はセクション本体を
  壊さず行単位のみ触る）

### GoogleCalendarWidget

今日の Google カレンダーの予定を表示する。

- 設定アクティビティ: `GoogleCalendarWidgetConfigActivity`（表示するカレンダー選択等）
- 受信: `GoogleCalendarWidgetReceiver`
- 更新: `GoogleCalendarWidget` + `CalendarWidgetUpdateWorker`
- 取得: `GoogleCalendarRepository` + `GetTodayCalendarEventsUseCase`

## 更新スケジューリング

| ファイル | 役割 |
|---|---|
| `widget/WidgetUpdateScheduler.kt` | WorkManager に定期更新をスケジュールするエントリポイント |
| `widget/BaseWidgetUpdateWorker.kt` | 共通のワーカー基底 |
| `widget/WidgetUpdateWorker.kt` | JournalTodoWidget の更新 |
| `widget/CalendarWidgetUpdateWorker.kt` | GoogleCalendarWidget の更新 |
| `widget/WidgetEntryPoint.kt` | Hilt 非対応の Glance から Repository を取り出す EntryPoint |
| `data/repository/WidgetConfigRepository.kt` | Widget ごとの設定値 |

## データフロー（JournalTodo の例）

1. WorkManager が `WidgetUpdateWorker` を起動。
2. `WidgetEntryPoint` から `JournalRepository` などを取得。
3. 今日のジャーナルを読み、`TodoParser` で `# TODO` セクションの行を抽出。
4. Glance Composable で描画。チェックタップ時は Action から `JournalRepository` を呼び、
   該当行を書き換えてから `update` を呼ぶ。

## 実装上の注意

- Glance は Compose UI と別ランタイム。`@Composable` は使えるが、Hilt 注入はできないので
  `WidgetEntryPoint` 経由で依存を取る。
- 設定アクティビティは `onCreate` 時点で `setResult(CANCELED)` を立てておき、
  ユーザが確定した場合のみ `OK` に差し替える AppWidget の慣例に従う。
- 書き込み系操作（TODO トグル）は I/O を含むため、`CoroutineWorker` 等で非同期に処理する。
