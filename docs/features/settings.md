# Settings（設定）

## 目的

Obsidian Vault の参照先、ファイル名フォーマット、権限、AI モデル可用性など、
アプリ全体の前提になる設定を集約する画面。
他の全機能はここで設定された URI やフォーマットを `ObsidianRepository` 経由で参照する。

## 画面の役割

- URI ピッカー: Vault ルート / Journal ディレクトリ / Pages ディレクトリ の選択。
- `filenameFormat` 編集（既定 `yyyy-MM-dd'.md'`）。
- パーミッション状態表示と再チェック: Calendar, 位置（Fine / Background）, ActivityRecognition, 通知, HealthConnect。
- AI モデル可用性の表示・ダウンロード導線。
- 画面復帰時に `refreshPermissions()` を呼ぶ想定。

## 構成要素

| レイヤ | ファイル | 役割 |
|---|---|---|
| UI | `ui/screen/settings/SettingsScreen.kt` | 画面 Composable |
| UI | `ui/screen/settings/SettingsViewModel.kt` | 設定・権限・AI 状態の仲介 |
| UI | `ui/screen/settings/SettingsUiState.kt` | 各種 URI・権限・AI 可用性 |
| Data | `data/repository/ObsidianRepository.kt` | Vault 設定（DataStore） |
| Data | `data/repository/LocationPermissionRepository.kt` | 位置権限 |
| Data | `data/repository/CalendarPermissionRepository.kt` | カレンダー権限 |
| Data | `data/repository/HealthRepository.kt` | HealthConnect 可用性・権限 |
| Data | `data/repository/AiRepository.kt` | AI モデル可用性・ダウンロード |

## データフロー

- `init` で各 Flow（Vault 系 URI、`filenameFormat`、権限、AI 可用性）を購読し UiState へ反映。
- 設定変更は `obsidianRepository.set*()` を即時呼び出し。永続化は Repository 側で DataStore に書く。
- 権限変更の検知はシステム依存のため、`refreshPermissions()` を明示的に呼んで反映させる。
- HealthConnect の `isAvailable` は Flow ではなく都度取得する（`refreshPermissions` 内で再評価）。

## 実装上の注意

- Vault URI は `ContentResolver` の永続パーミッションが前提。取得・解放は Repository 側の責務。
- `filenameFormat` を変えると過去ジャーナルが見えなくなることがあるので、UI では
  例を出すなど誤入力に強い表示を推奨。
- この画面は副作用が広いので、ロジックは最小限に保ち、各 Repository が真値を持つ構造を維持する。
