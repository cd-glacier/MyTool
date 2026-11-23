# HouseholdPoints（家事ポイント管理）

## 目的

[Household](./household.md) 機能で記録する「家事ごとのポイント配点」を管理する画面。
家事名とポイント値のリストを CRUD で編集し、Obsidian の pages フォルダ配下に
Markdown として永続化する。

## 画面の役割

- ポイント一覧の表示。
- 追加: 家事名＋ポイントの入力フォーム。
- 編集: 既存エントリのポイント値のインライン編集。
- 削除: 削除確認ダイアログ経由。
- 入力エラー時はスナックバー相当のメッセージを出す。

## 構成要素

| レイヤ | ファイル | 役割 |
|---|---|---|
| UI | `ui/screen/householdpoints/HouseholdPointsScreen.kt` | 画面 Composable |
| UI | `ui/screen/householdpoints/HouseholdPointsViewModel.kt` | CRUD と永続化 |
| UI | `ui/screen/householdpoints/HouseholdPointsUiState.kt` | リスト・新規フォーム・編集／削除状態 |
| Domain | `domain/model/HouseholdPoint.kt` | `(name, points)` |
| Domain | `domain/usecase/GetHouseholdPointsUseCase.kt` | 読み込み |
| Domain | `domain/usecase/SaveHouseholdPointsUseCase.kt` | 保存 |
| Domain | `domain/usecase/HouseholdPointParser.kt` | Markdown ⇄ `HouseholdPoint` |
| Data | `data/repository/HouseholdRepository.kt` | pages フォルダへの I/O |

## データフロー

### 読み込み（refresh）

- `pagesDirUri` が未設定なら空リストで終了。
- `GetHouseholdPointsUseCase()` で現在のポイント定義を取得。

### 追加 / 編集 / 削除

1. バリデーション（名前空白不可・ポイントは `Int`、追加時は同名重複不可）。
2. 新しいリストを組み立てて `SaveHouseholdPointsUseCase(updated)` へ渡し、
   `HouseholdRepository` が Markdown に上書き保存。
3. 成功時は UiState の `points` を即時更新し入力フォームをクリア。失敗時は `errorMessage` を立てる。

## 実装上の注意

- 保存は毎操作で全件書き込み（小規模リストなので差分管理しない）。
- Household 画面では「使用頻度順」で並び替えて表示されるため、ここでの並び順は保存順。
- `pagesDirUri` 未設定時は操作しても何も起こらないので、UI 側で設定への導線が必要。
