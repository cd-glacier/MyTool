# CopyObsidianJournal（ジャーナルコピー）

## 目的

前日などの既存ジャーナルを雛形にして、新しい日のジャーナルファイルを生成する。
その際、翌日に引き継ぐべきでない要素（完了済み TODO、チェック済み習慣、
日次追記セクション）は `JournalTransformer` によって除去／リセットされる。
手動実行に加え、`AutoCopyJournalScheduler` によって日付変更時の自動生成にも使う。

全体フォーマットと cleanup ルールは [../obsidian-journal.md](../obsidian-journal.md) を参照。

## 画面の役割

- コピー元日付・コピー先日付の選択（既定: 前日 → 今日）。
- `[COPY]` 実行。先に `CheckJournalTargetUseCase` でコピー先に中身があるか確認し、
  あれば上書き確認ダイアログを出す。
- 自動コピー（AutoCopy）トグル。有効化すると `AutoCopyJournalScheduler` が
  WorkManager で日次実行をスケジュールする。
- 完了時にスナックバー表示。実行時間が 2 秒未満ならあえて 2 秒待ってから表示する
  （視認性のため）。

## 構成要素

| レイヤ | ファイル | 役割 |
|---|---|---|
| UI | `ui/screen/copyjournal/CopyObsidianJournalScreen.kt` | 画面 Composable |
| UI | `ui/screen/copyjournal/CopyObsidianJournalViewModel.kt` | 日付操作・コピー実行・AutoCopy トグル |
| UI | `ui/screen/copyjournal/CopyObsidianJournalUiState.kt` | 日付・設定・ダイアログ・スナックバー状態 |
| Domain | `domain/usecase/CopyJournalUseCase.kt` | 本体: ソース読込→`JournalTransformer`→ターゲット書込 |
| Domain | `domain/usecase/CheckJournalTargetUseCase.kt` | ターゲットに有意な内容があるか判定 |
| Domain | `domain/usecase/JournalTransformer.kt` | 管理ブロック内 cleanup（セクション別） |
| Data | `data/repository/JournalRepository.kt` | Markdown の読み書き |
| Data | `data/repository/AutoCopyJournalScheduler.kt` | WorkManager による自動実行 |
| Data | `data/repository/ObsidianRepository.kt` | `journalDirUri`, `filenameFormat`, `autoCopyEnabled` |

## データフロー

### 手動コピー（onCopy）

1. `CheckJournalTargetUseCase` でターゲット日ファイルの有無・内容を確認。
2. 中身があれば上書き確認ダイアログを出す。
3. `CopyJournalUseCase` が
   - ソース日を `JournalRepository.readContent()` で読み、
   - `JournalTransformer.transform()` で管理ブロック内を翌日用に変換し、
   - ターゲット日に `JournalRepository.writeContent()` で書き込む。
4. 結果をスナックバーで通知。

### 自動コピー

- Settings で AutoCopy を有効化すると `AutoCopyJournalScheduler.enable()` が
  WorkManager に定期ジョブを登録する。日付が変わるタイミングで前日→当日のコピーを
  実行する想定。

## 実装上の注意

- `JournalTransformer` は管理ブロック内のみを触る。フリー領域はそのままコピーされる。
- 上書き時はターゲット側の既存ファイルを完全に置換する。
