# Diary（日記）

## 目的

日々の出来事・考えを短文でその場で記録し、Obsidian の日次ジャーナル（`YYYY-MM-DD.md`）の
`# Diary` セクションに時刻付きで追記する。入力の UI は MyTool 内に持ちつつ、
データの正本は Obsidian の Markdown ファイル側に置くことで、
PC からも Obsidian で通常の日記として閲覧・編集できる状態を保つ。

一覧表示では、Vault 内の過去ジャーナルから Diary セクションを抽出し、
日付ごとにまとめて時系列で振り返れるようにする。

## 画面の役割

- **入力フォーム（ADD_DIARY）**: 本文を入力して `[+ADD]` でその時点の時刻（`HH:mm`）とともに今日分のジャーナルに追記する。
- **一覧**: 日付セクションごとに過去の Diary エントリを表示する。画面復帰時（`LifecycleResumeEffect`）に Vault を再スキャンする。

## 構成要素

| レイヤ | ファイル | 役割 |
|---|---|---|
| UI | `ui/screen/diary/DiaryScreen.kt` | 画面 Composable（入力フォーム＋日付別リスト） |
| UI | `ui/screen/diary/DiaryViewModel.kt` | 状態保持、追加・再スキャンの起点 |
| UI | `ui/screen/diary/DiaryUiState.kt` | `sections`, `addForm`, `isLoading` を保持 |
| UI | `ui/component/DiaryItem.kt` | 1 エントリの表示（`DiaryItemUiModel`） |
| Domain | `domain/usecase/AddDiaryToJournalUseCase.kt` | ジャーナルへの追記＋ローカルキャッシュ更新 |
| Domain | `domain/usecase/ScanDiariesUseCase.kt` | Vault 内ジャーナルから Diary を抽出しキャッシュに反映 |
| Domain | `domain/usecase/DiaryParser.kt` | Markdown からエントリ列への変換 |
| Data | `data/repository/DiaryRepository.kt` | 抽出済みエントリのローカル保持（`sections: Flow`） |
| Data | `data/repository/JournalRepository.kt` | ジャーナル Markdown の読み書き |

## データフロー

### 追加（submitAdd）

1. `DiaryViewModel.submitAdd()` が本文を trim し、`HH:mm` のタイムスタンプを生成する。
2. `AddDiaryToJournalUseCase` を呼ぶ。
3. UseCase は `JournalRepository.readContent()` で今日分の Markdown を取得し、
   `JournalSectionWriter.write(original, JournalSection.DIARY, entryBlock)` で
   `# Diary` セクションに追記する（セクション順序・管理ブロックのルールは
   `.claude/rules/journal-format.md` を参照）。
4. 書き戻し後、`DiaryRepository.replaceForDate()` でメモリ上のキャッシュも更新する。
5. 成功時は入力フォームをクリアし `refresh()` で再スキャンする。

### エントリブロックの形式

複数行の本文は 2 行目以降をインデントして 1 つのリストアイテムにまとめる。

```
- HH:mm 1行目本文
  2行目本文
  3行目本文
```

### 一覧取得（refresh / init）

- `DiaryViewModel.init` で `DiaryRepository.sections` を購読し、UiState に反映する。
- `refresh()` は `ScanDiariesUseCase` を呼び、Vault 内の全ジャーナルを走査して
  `DiaryParser` で各ファイルの `# Diary` セクションをパースし、Repository のキャッシュを置き換える。
- 画面復帰時に自動で呼ばれる。多重実行は `AtomicBoolean` でガードする。

## 翌日への引き継ぎ

Diary セクションは「追記型」。翌日ジャーナル生成時（`JournalTransformer`）には
セクションごと削除され、翌日は空から始まる。詳細は `.claude/rules/journal-format.md` §5。
