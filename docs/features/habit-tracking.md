# HabitTracking（習慣トラッキング）

## 目的

日課として続けたい習慣（例: 運動、服薬、学習など）のチェックリストを、
Obsidian の日次ジャーナル `# HabitTracking` セクションに保持し、
MyTool からワンタップでチェック／アンチェックできるようにする。

習慣の**定義**（項目と頻度）はユーザが Obsidian 上で手書きして管理し、
MyTool はその日の達成状態（`- [ ]` / `- [x]`）のトグルのみを担当する。
これにより、PC からの編集と Android からの日々のチェック運用を両立させる。

## セクションのフォーマット

ジャーナル管理ブロック内の `# HabitTracking` セクションは、
`##` 見出しで頻度を区切り、その下にチェックボックスを並べる。

```markdown
# HabitTracking

## daily
- [ ] 筋トレ
- [x] 服薬

## every monday
- [ ] 洗濯

## every friday
- [ ] 資源ごみ
```

- 頻度の書式: `daily` または `every {monday..sunday}`（小文字）。
- 項目: `- [ ] 名前` または `- [x] 名前`。
- セクション終端: 次の `# ` 見出し、または管理ブロックの閉じ `---`。

翌日ジャーナル生成時（`JournalTransformer`）には `- [x]` が `- [ ]` に戻される。
詳細は `.claude/rules/journal-format.md` §5。

## 画面の役割

- 日付ナビゲーション（前日／翌日。今日より未来へは進めない）。
- その日の曜日で有効な習慣のみをリスト表示（`daily` と該当曜日の `every ...`）。
- チェックのタップで即座に UI 反映（楽観的更新）し、裏でジャーナルを書き換える。失敗時は再読込して差分を戻す。

## 構成要素

| レイヤ | ファイル | 役割 |
|---|---|---|
| UI | `ui/screen/habit/HabitTrackingScreen.kt` | 画面 Composable（日付バー＋習慣リスト） |
| UI | `ui/screen/habit/HabitTrackingViewModel.kt` | 日付切替・トグル・読み込み |
| UI | `ui/screen/habit/HabitTrackingUiState.kt` | `date`, `habits`, `isLoading`, `journalConfigured`, `errorMessage` |
| Domain | `domain/model/Habit.kt`, `HabitFrequency.kt` | 習慣と頻度（`Daily` / `Weekly(DayOfWeek)`） |
| Domain | `domain/usecase/GetTodayHabitsUseCase.kt` | その日のジャーナルから習慣を抽出し、曜日でフィルタ |
| Domain | `domain/usecase/ToggleHabitUseCase.kt` | 該当行のチェック状態をトグルして書き戻し |
| Domain | `domain/usecase/HabitParser.kt` | Markdown ⇄ `Habit` のパースとトグル |
| Data | `data/repository/JournalRepository.kt` | ジャーナル Markdown の読み書き |

## データフロー

### 読み込み（refresh / onDateChange）

1. `ObsidianRepository` から `journalDirUri` と `filenameFormat` を取得。
2. 未設定なら `journalConfigured = false` にして終了。
3. `GetTodayHabitsUseCase` が対象日のジャーナルを読み、`HabitParser.parse()` で全習慣を取り出し、
   `Habit.appliesTo(dayOfWeek)` でその曜日に該当するものだけを返す。
4. 結果を UiState に反映する。

### トグル（onHabitToggle）

1. UiState を先に反転させ、UI を即時更新する（楽観的更新）。
2. `ToggleHabitUseCase` を呼ぶ。UseCase は現在のジャーナルを読み、`HabitParser.toggle()` で
   `name` と `frequency` の両方が一致する最初の行の `[ ]` / `[x]` を反転させて書き戻す。
3. 該当行が見つからない／書き込み失敗なら `errorMessage` を立て、`loadHabits(showLoading=false)`
   で再読込して UI の状態をジャーナル側の真値に戻す。

## 実装上の注意

- MyTool は `# HabitTracking` セクションを**追加・削除しない**（`CLEANUP_ONLY` 扱い）。
  セクション自体が無いジャーナルでは空リストとして振る舞う。
- 頻度のパースは小文字限定。大文字・日本語表記は非対応（意図的な制約）。
- `HabitParser.toggle` は最初に一致した 1 行のみ変更する。同名・同頻度の重複行は想定しない。
