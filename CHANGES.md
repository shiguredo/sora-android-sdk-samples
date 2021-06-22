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

### FIX

- 自身の映像プレビューが反転している問題を修正する

### UPDATE

- sdk が channel_id を必須にした変更に追従する
- spotlight_number は未指定をデフォルトにする

## 2021.1

### UPDATE

- SDK のバージョンを 2021.1 に上げる
- 新しいスポットライトに対応する
- Kotlin を 1.4.31 に更新する
- Gradle を 6.8.3 に更新する
- 依存ライブラリを更新する
  - com.github.ben-manes:gradle-versions-plugin を 0.38.0 に更新する
  - com.android.tools.build:gradle を 4.1.2 に更新する
  - com.google.android.material:material を 1.3.0 に更新する
  - androidx.navigation:navigation-fragment-ktx を 2.3.3 に更新する
  - androidx.navigation:navigation-ui-ktx を 2.3.3 に更新する
  - jp.co.cyberagent.android:gpuimage を 2.1.0 に更新する

### ADD

- サイマルキャスト画面を新規に追加する
- データチャネルシグナリングに対応する
  - ビデオチャット、ボイスチャット、サイマルキャスト、スポットライトが対象
  - data_channel_signlaing, ignore_disconnect_websocket パラメータ設定を追加する
  - @shino

### CHANGE

- 新しいスポットライトがデフォルトで利用されるように修正する
- サイマルキャスト画面の追加に伴い、 video chat room からサイマルキャストの設定を削除する
- スポットライト画面の映像コーデックから VP9 を外す
- 設定項目名を日本語に変更する
- 音声コーデックから PCMU を外す

### FIX

- ボイスチャット画面でマルチストリームが無効にできない問題を修正する
- 音声のみを受信するよう設定したにも関わらず、映像を受信してしまう問題を修正する
- スクリーンキャスト画面がクラッシュしていた問題を修正する
  - Android 10 からは、特定のサービスを定義する際に、マニフェストに foregroundServiceType を定義する必要がある
  - 参考: https://developer.android.com/about/versions/10/features?hl=ja#fg-service-types
- ビデオチャット画面の起動時に縦固定となる問題を修正する
- 各画面で端末回転に追随しない問題を修正する

## 2020.1

## 1.10.0

- SDK のバージョンを 2020.1 に上げる

### UPDATE

- Android Studio 3.5.2 に対応する
- Kotlin を 1.3.50 に上げる
- `androidx.appcompat:appcompat` を 1.1.0` に上げる

### ADD

- video chat room にステレオで配信するオプションを追加する
  - Andoird 9 / Pixel3 XL からの配信で動作を確認している
    - カメラを体の前に持った状態で、液晶を自分向き、ホームボタン側を自分から見て右にした状態
    - 映像はリアカメラからの入力
    - マイクは内蔵を利用し、上部が左、下部を右とすると映像の向きとステレオの左右が同期する

### CHANGE

- Plan-B 対応を削除する
- minSdkVersion を 21 に上げる

## 1.9.0

### UPDATE

- SDK のバージョンを 1.9.0 に上げる
- Kotlin を 1.3.41 に上げる
- PermissionsDispatcher を 4.5.0 に上げる
- Android Studio 3.4.2 に対応する

### ADD

- video chat room に simulcast の設定を追加する
- video chat room に upstream video の latency 関連統計ログを追加する
- video chat room に `SoraAudioOption` を明示的に記述する
- video chat room, spotlight chat room, voice chat room に音声ビットレート設定を追加する

### CHANGE

- video chat room, effected video chat room, spotlight room, voice chat room で
  `MODE_IN_COMMUNICATION` を使うように変更する
- spotlight の映像ビットレートを 1000 に変更する


## 1.8.1

### UPDATE

- SDK のバージョンを 1.8.1 に上げる
- Video chat の connect metadata を Any? 型に変更する
- Kotlin を 1.3.30 に上げる
- PermissionsDispatcher を 4.3.1 に上げる
- Android Studio 3.4 に対応する

### ADD

- Video chat に client ID を指定する選択肢を追加する
- Video chat に解像度固定の選択肢を追加する
- Video chat のビットレートに 10Mbps, 15Mbps, 20Mbps, 30Mbps を追加する
  - 15Mbps までが Sora のサポート範囲
- Video chat に signalingNotifyMetadata を追加する

## 1.8.0

### UPDATE

- SDK のバージョンを 1.8.0 に上げる
- `android:extractNativeLibs` を false に設定した
- Kotlin を 1.3.20 に上げる
- Anko を依存から外す
- Android support library から androidx に移行する
- PermissionsDispatcher を 4.3.0 に上げる
- compileSdkVersion, targetSdkVersion を 28 に上げる
- video chat room に video enable オプションを追加する
- Android Studio 3.3 に対応する
- `jp.co.cyberagent.android:gpuimage` を 2.0.3 に上げる

### CHANGE

- SDP semantics の選択肢のデフォルト値を Unified Plan に変更する
  - upstream のシグナリングで audio や video が false の場合でも、他の配信者の
    audio や video のトラックを受信する SDP が Sora から offer されるように変わる
  - Plan B のときには audio false のときには audio track が SDP に含まれず、
    video が false のときには video のトラックが含まれなかった
    - Plan B の制限による挙動だった

### ADD

- Effected video chat にセピアトーン化のエフェクトを追加する
  - Thanks to @daneko
- Effected video chat にデバッグ、比較用としてなにもしないエフェクトを追加する
  - Thanks to @daneko

### CHANGE

- スポットライトルームの初期映像コーデックを VP9 に変更する

### FIX

- Effected video chat で I420 から変換された RGB データがずれていた問題を修正する
  - これに伴い、NV12/NV21 の経由を廃止し、I420 と RGBA の直接の相互変換する
  - Thanks to @daneko

## 1.7.1

### ADD

- Video chat, Voice chat, Spotlight の各セットアップに sdpSemantics 選択肢を追加する
  - ただし、Sora Android SDK の動作確認は Plan B のみで Unified Plan は試験的実装
  - Voice chat は Unified Plan 選択時にエラーで接続できない

### UPDATE

- SDK のバージョンを 1.7.1 に上げる
- Kotlin を 1.2.71 に上げる
- CircleCI のコンテナに入っている Android NDK を使うよう変更する
- ビルドメモリ設定を `gradle.properties` から `JVM_OPTS: -Xmx3200m` に変更する
- Android Studio 3.2.1 に対応する

## 1.7.0

### CHANGE

- SoraScreencastSerivce の起動中を companion object の変数で管理するよう変更する
- SDK のバージョンを 1.7.0 に上げる
- Android Studio 3.1.4 に対応する
- SDK から Service 状態管理ユーティリティが削除されたため、自前で companion object での管理に変更する

## 1.6.0

### UPDATE

- SDK のバージョンを 1.6.0 に上げる
- Android Studio 3.1.3 に対応する
- PermissionsDispatcher を 3.2.0 に上げる
  - lint バグフィックスにより不要な SuppressLint アノテーションを削除する
- Kotlin を 1.2.51 に上げる
- START ボタンをオプションリストの上に移動する
- CircleCI キャッシュを利用しない
  - ときどきビルドが失敗するが、キャッシュ利用しないと成功するため
  - NDK セットアップに 37 sec, androidDependencies に 43 sec 程度
  - ただしキャッシュがビルド失敗の根本原因かは不明
- Anko を 0.10.5 に上げる
- スクリーンキャスト画面を `TYPE_APPLICATION_OVERLAY` に変更する
  - `TYPE_PHONE` が deprecated になったため
- Android 8 の Notification Channel に対応する
- Video chat, Voice chat, Spotlight chat, effected video chat の音量を
  ボリュームキーから制御できるようにする
- 解像度オプションを増やした
- PermissionsDispatcher を 3.3.1 に上げる
- スポットライトルームの初期映像コーデックを VP8 に変更する
- ボリューム変更対象ストリームを `STREAM_VOICE_CALL` に変更する

### ADD

- スポットライト機能のデモを追加する
  - 通信の方向は BIDIRECTIONAL(upstream) と MULTI_DOWN(downstream) を選択可能
  - メディアは映像+音声か音声のみを選択可能

### CHANGE

- MediaStream#label() の代わりに id を使うよう変更する

### FIX

- スクリーンキャストの停止時に SIGABRT が発生していた問題を修正する

## 1.5.3

### UPDATE

- SDK のバージョンを 1.5.4 に上げる

## 1.5.2

### CHANGE

- onByteBufferFrameCaptured が onFrameCaptured が置き換えられた変更に対応する
  - cf. https://webrtc-review.googlesource.com/c/src/+/43022

- audio disabled のときは upstream/downstream ともに音声は無効にする
- SoreRemoteRendererSlot の誤植を修正する

### UPDATE

- SDK のバージョンを 1.5.3 に上げる
- CI 環境の NDK を r14 から r16 に上げる

## 1.5.1

### UPDATE

- SDK のバージョンを 1.5.2 に上げる
- kotlin ソースディレクトリの名前を kotlin に変更する
- PermissionDispatcher 3.x に対応する
- Kotlin 1.2.30 に上げる

## 1.5.0

### UPDATE

- SDK のバージョンを 1.5.0 に上げる

## 1.4.3

### FIX

- SDK のバージョンをほんとうに 1.4.1 に上げる

## 1.4.2

### UPDATE

- SDK のバージョンを 1.4.1 に上げる

### UPDATE

## 1.4.1

### UPDATE

- Android support library を 26.0.2 に上げる
- PermissionsDispatcher を 3.1.0 に上げる

## 1.4.0

### UPDATE

- SDK のバージョンを 1.4.0 に上げる
- Android Studio 3.0 に対応する
  - gradle: 4.1
  - android-maven-gradle-plugin: 2.0
- Kotlin 1.2.10 に上げる

## 1.3.1

### UPDATE

- SDK のバージョンを 1.3.1 に上げる
- Kotlin を 1.1.51 に上げる
- CircleCI でのビルドを設定する
- コマンドラインビルドのエラー回避のため、 org.jetbrains:annotations をcompile 依存から除外する

### CHANGE

- Signaling Endpoint の設定を Config.kt から build.gradle に移動した

## 1.3.0

### UPDATE

- SDK のバージョンを上げた

### FIX

- screencast で multistream が有効にならない現象を修正する
- video chat で single down のときにリモートストリームが表示されない現象を修正する

## 1.2.0

### UPDATE

- SDK のバージョンを上げた

## 1.1.0

### UPDATE

- 依存ライブラリのバージョンを上げた

### ADD

- Sora Android SDK 依存を JitPack 経由とし、AAR の手動ダウンロードを不要にする

## 1.0.0

最初のリリース
