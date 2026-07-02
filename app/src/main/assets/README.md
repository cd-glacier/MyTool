# Assets

## universal_sentence_encoder.tflite

MediaPipe Text Embedder 用のモデルファイル。以下からダウンロードしてこのディレクトリに配置する。

- https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite

配置しないと RECIPE 画面の検索インデックス生成が失敗する（Worker は Result.retry で終わる）。
