# MoneyBook（家計簿）

## 目的

月次の収入・カード支出・その他支出・予算・貯蓄・年額サービスを
Obsidian Vault 上の Markdown（`MoneyBook`）として管理する家計簿機能。
PC の Obsidian でも同じファイルを直接編集できることを前提に、
MyTool 側は使い勝手のよい編集 UI と自動計算を提供する。

## 画面の役割

- 月切替（前月／翌月）。該当月が存在しなければ前月の項目名を引き継いで新規作成する
  （`MoneyCalculator.carryForwardNames`）。
- カテゴリ別のリスト編集: 収入 / カード支出 / その他 / 予算 / 貯蓄 / 年額サービス。
- 各項目の追加・編集・アーカイブ（物理削除せず `archived` 状態）。
- 入力に応じた 800ms デバウンス付き自動保存（`scheduleAutoSave`）。
- 保存失敗時はエラーメッセージを表示。

## 構成要素

| レイヤ | ファイル | 役割 |
|---|---|---|
| UI | `ui/screen/money/MoneyScreen.kt` | 画面 Composable（カテゴリごとのセクション） |
| UI | `ui/screen/money/MoneyViewModel.kt` | 編集・月切替・自動保存 |
| UI | `ui/screen/money/MoneyUiState.kt` | `book`, `displayedMonth`, `isLoading`, `saveError` |
| UI | `ui/screen/money/chart/` | グラフ表示コンポーネント |
| Domain | `domain/model/MoneyBook.kt`, `MonthlyMoney.kt`, `MoneyItem.kt`, `SavingsItem.kt`, `AnnualService.kt`, `MoneyCategory.kt` | ドメインモデル |
| Domain | `domain/usecase/MoneyCalculator.kt` | 項目の繰越・合計計算 |
| Data | `data/repository/MoneyRepository.kt` | `MoneyBook` の `observe` / `save` |
| Data | `data/repository/MoneyMarkdown.kt` | Markdown ⇄ `MoneyBook` |

## データフロー

### 読み込み

1. `obsidianRepository.vaultUri` を購読して Vault 設定の有無を UiState に反映。
2. `moneyRepository.observeBook()` を購読。表示月が無ければ
   `ensureMonthExists()` で前月の項目名を引き継いで追加。

### 編集

- カテゴリごとに `update{Category}` / `add{Category}` / `remove{Category}` を持つ。
- 削除は `archived` マップへ名前を移すソフトデリート（他月への再登場を防ぐ）。
- `AnnualService` は `archived = true` フラグで非表示化。

### 自動保存

- 編集があるたびに `scheduleAutoSave` が 800ms デバウンスで `MoneyRepository.save(book)` を呼ぶ。
- 保存失敗は `saveError` に積む。

## 実装上の注意

- Vault 側のファイル形式は `MoneyMarkdown` が担保する。手書き編集との両立が前提のため、
  パーサ・ライタ両方を壊さないよう変更には注意。
- 月を跨ぐときの「項目名の繰越」は `MoneyCalculator.carryForwardNames` に集約。
- `replaceAt` などの小さなリスト操作はファイル末尾のトップレベル関数で定義。
