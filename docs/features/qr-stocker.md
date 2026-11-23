# QrStocker（QR ストッカー）

## 目的

ポイントカード・会員証などの QR コードを一度撮影して保存しておき、
必要なときに同じ QR を再生成して提示できるようにする。
画像ファイルのままではなく、QR のテキスト内容と生成パラメータ（誤り訂正レベル・モードなど）を
保存するため、再生成時はきれいな QR が得られる。

## 画面の役割

- 一覧（タイトル付き）。タップで詳細（QR 生成・表示）。
- 画像ピッカーで QR 画像を選択 → デコード → タイトル入力 → 保存。
- デコード／保存中はローディング、失敗時はエラーメッセージ。

## 構成要素

| レイヤ | ファイル | 役割 |
|---|---|---|
| UI | `ui/screen/qrstocker/QrStockerScreen.kt` | 一覧・画像ピッカー・保存ダイアログ |
| UI | `ui/screen/qrstocker/QrStockerViewModel.kt` | デコード・保存・一覧購読 |
| UI | `ui/screen/qrstocker/QrStockerUiState.kt` | `entries`, `pendingDecoded`, `isLoading`, `error` |
| UI | `ui/screen/qrstocker/detail/` | 詳細画面（QR 再生成・表示） |
| Domain | `domain/model/QrEntry.kt`, `QrEcLevel.kt` | QR エントリとパラメータ |
| Domain | `domain/usecase/DecodeQrImageUseCase.kt` | 画像 URI → テキスト・ECLevel・モード |
| Domain | `domain/usecase/GenerateQrBitmapUseCase.kt` | エントリ → Bitmap |
| Data | `data/repository/QrStockerRepository.kt` | 永続化（`observe`, `load`, `upsert`） |
| Data | `data/repository/QrImageRepository.kt` | 画像 I/O |
| Data | `data/repository/QrMarkdown.kt` | Markdown ⇄ `QrEntry` |

## データフロー

### 読み込み

- `init` で `observeEntries()` を購読。`refresh()` で `load()` を明示的に再取得。

### 追加（onImagePicked → onConfirmPending）

1. `DecodeQrImageUseCase(uri)` で画像から QR を復号。内容・ECLevel・モードを取得。
2. `pendingDecoded` に保持し、タイトル入力をユーザから受ける。
3. `onConfirmPending` でタイトルを添えて `QrEntry` を生成し `qrStockerRepository.upsert(entry)`。
4. 成功後に `refresh()` し、失敗時はダイアログを維持してエラー表示。

### 詳細

- タイトル・内容・パラメータから `GenerateQrBitmapUseCase` で QR を再生成し表示する。

## 実装上の注意

- 元画像は保存せず、デコード済みのテキストとパラメータのみを保持する方針。
- `QrEcLevel` を文字列として UiState に持つため、確定時に `valueOf` で復元する。
- `version` は保存時 `null`（必要になれば `QrEntry` に拡張）。
