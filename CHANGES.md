# 変更履歴

- UPDATE
    - 下位互換がある変更
- ADD
    - 下位互換がある追加
- CHANGE
    - 下位互換のない変更
- FIX
    - バグ修正


## develop

### UPDATE

- Kotlin を 1.3.41 に上げた
- PermissionsDispatcher を 4.5.0 に上げた
- Android Studio 3.4.2 に対応した

### ADD

- video chat room に simulcast の設定を追加した
- video chat room に upstream video の latency 関連統計ログを追加した
- video chat room に `SoraAudioOption` を明示的に記述した
- video chat room, spotlight chat room, voice chat room に音声ビットレート設定を追加した

### CHANGE

- video chat room, effected video chat room, spotlight room, voice chat room で
  `MODE_IN_COMMUNICATION` を使うように変更した


## 1.8.1

### UPDATE

- SDK のバージョンを 1.8.1 に上げた
- Video chat の connect metadata を Any? 型に変更した
- Kotlin を 1.3.30 に上げた
- PermissionsDispatcher を 4.3.1 に上げた
- Android Studio 3.4 に対応した

### ADD

- Video chat に client ID を指定する選択肢を追加した
- Video chat に解像度固定の選択肢を追加した
- Video chat のビットレートに 10Mbps, 15Mbps, 20Mbps, 30Mbps を追加した
  - 15Mbps までが Sora のサポート範囲
- Video chat に signalingNotifyMetadata を追加した

## 1.8.0

### UPDATE

- SDK のバージョンを 1.8.0 に上げた
- `android:extractNativeLibs` を false に設定した
- Kotlin を 1.3.20 に上げた
- Anko を依存から外した
- Android support library から androidx に移行した
- PermissionsDispatcher を 4.3.0 に上げた
- compileSdkVersion, targetSdkVersion を 28 に上げた
- video chat room に video enable オプションを追加した
- Android Studio 3.3 に対応した
- `jp.co.cyberagent.android:gpuimage` を 2.0.3 に上げた

### CHANGE

- SDP semantics の選択肢のデフォルト値を Unified Plan に変更した
  - upstream のシグナリングで audio や video が false の場合でも、他の配信者の
    audio や video のトラックを受信する SDP が Sora から offer されるように変わります。
  - Plan B のときには audio false のときには audio track が SDP に含まれず、
    video が false のときには video のトラックが含まれませんでした。
    これは Plan B の制限による挙動でした。

### ADD

- Effected video chat にセピアトーン化のエフェクトを追加した
  - Thanks to @daneko
- Effected video chat にデバッグ、比較用としてなにもしないエフェクトを追加した
  - Thanks to @daneko

### CHANGE

- スポットライトルームの初期映像コーデックを VP9 に変更した

### FIX

- Effected video chat で I420 から変換された RGB データがずれていた問題を修正した
  - これに伴い、NV12/NV21 の経由を廃止し、I420 と RGBA の直接の相互変換とした
  - Thanks to @daneko

## 1.7.1

### ADD

- Video chat, Voice chat, Spotlight の各セットアップに sdpSemantics 選択肢を追加した
  - ただし、Sora Android SDK の動作確認は Plan B のみで Unified Plan は試験的実装
  - Voice chat は Unified Plan 選択時にエラーで接続できない

### UPDATE

- SDK のバージョンを 1.7.1 に上げた
- Kotlin を 1.2.71 に上げた
- CircleCI のコンテナに入っている Android NDK を使うよう変更した
- ビルドメモリ設定を `gradle.properties` から `JVM_OPTS: -Xmx3200m` に変更した
- Android Studio 3.2.1 に対応した

## 1.7.0

### CHANGE

- SoraScreencastSerivce の起動中を companion object の変数で管理するよう変更した
- SDK のバージョンを 1.7.0 に上げた
- Android Studio 3.1.4 に対応した
- SDK から Service 状態管理ユーティリティが削除されたため、自前で companion object での管理に変更した

## 1.6.0

### UPDATE

- SDK のバージョンを 1.6.0 に上げた
- Android Studio 3.1.3 に対応した
- PermissionsDispatcher を 3.2.0 に上げた
  - lint バグフィックスにより不要な SuppressLint アノテーションを削除した
- Kotlin を 1.2.51 に上げた
- START ボタンをオプションリストの上に移動した
- CircleCI キャッシュを利用しない
  - ときどきビルドが失敗するが、キャッシュ利用しないと成功するため
  - NDK セットアップに 37 sec, androidDependencies に 43 sec 程度
  - ただしキャッシュがビルド失敗の根本原因かは不明
- Anko を 0.10.5 に上げた
- スクリーンキャスト画面を `TYPE_APPLICATION_OVERLAY` に変更した
  - `TYPE_PHONE` が deprecated になったため
- Android 8 の Notification Channel に対応した
- Video chat, Voice chat, Spotlight chat, effected video chat の音量を
  ボリュームキーから制御できるようにした
- 解像度オプションを増やした
- PermissionsDispatcher を 3.3.1 に上げた
- スポットライトルームの初期映像コーデックを VP8 に変更した
- ボリューム変更対象ストリームを `STREAM_VOICE_CALL` に変更した

### ADD

- スポットライト機能のデモを追加した
  - 通信の方向は BIDIRECTIONAL(upstream) と MULTI_DOWN(downstream) を選択可能
  - メディアは映像+音声か音声のみを選択可能

### CHANGE

- MediaStream#label() の代わりに id を使うよう変更した

### FIX

- スクリーンキャストの停止時に SIGABRT が発生していた問題を修正した

## 1.5.3

### UPDATE

- SDK のバージョンを 1.5.4 に上げた

## 1.5.2

### CHANGE

- onByteBufferFrameCaptured が onFrameCaptured が置き換えられた変更に対応した
  - cf. https://webrtc-review.googlesource.com/c/src/+/43022

- audio disabled のときは upstream/downstream ともに音声は無効にする
- SoreRemoteRendererSlot の誤植を修正した

### UPDATE

- SDK のバージョンを 1.5.3 に上げた
- CI 環境の NDK を r14 から r16 に上げた

## 1.5.1

### UPDATE

- SDK のバージョンを 1.5.2 に上げた
- kotlin ソースディレクトリの名前を kotlin に変更した
- PermissionDispatcher 3.x に対応した
- Kotlin 1.2.30 に上げた

## 1.5.0

### UPDATE

- SDK のバージョンを 1.5.0 に上げた

## 1.4.3

### FIX

- SDK のバージョンをほんとうに 1.4.1 に上げた

## 1.4.2

### UPDATE

- SDK のバージョンを 1.4.1 に上げた

### UPDATE

## 1.4.1

### UPDATE

- Android support library を 26.0.2 に上げた
- PermissionsDispatcher を 3.1.0 に上げた

## 1.4.0

### UPDATE

- SDK のバージョンを 1.4.0 に上げた
- Android Studio 3.0 に対応した
  - gradle: 4.1
  - android-maven-gradle-plugin: 2.0
- Kotlin 1.2.10 に上げた

## 1.3.1

### UPDATE

- SDK のバージョンを 1.3.1 に上げた
- Kotlin を 1.1.51 に上げた
- CircleCI でのビルドを設定した
- コマンドラインビルドのエラー回避のため、 org.jetbrains:annotations をcompile 依存から除外した

### CHANGE

- Signaling Endpoint の設定を Config.kt から build.gradle に移動した

## 1.3.0

### UPDATE

- SDK のバージョンを上げた

### FIX

- screencast で multistream が有効にならない現象を修正した
- video chat で single down のときにリモートストリームが表示されない現象を修正した

## 1.2.0

### UPDATE

- SDK のバージョンを上げた

## 1.1.0

### UPDATE

- 依存ライブラリのバージョンを上げた

### ADD

- Sora Android SDK 依存を JitPack 経由とし、AAR の手動ダウンロードを不要にすた

## 1.0.0

最初のリリース
