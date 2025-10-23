# 変更履歴

- CHANGE
  - 下位互換のない変更
- UPDATE
  - 下位互換がある変更
- ADD
  - 下位互換がある追加
- FIX
  - バグ修正

## 2025.3

- [CHANGE] SoraVideoChannel の `hardMuted/softMuted` プロパティ名を `cameraHardMuted/cameraSoftMuted` に変更する破壊的変更
  - オーディオ用の `audioHardMuted/audioSoftMuted` プロパティを追加に伴いコードの可読性を上げるため
- [UPDATE] 前面・背面カメラの切り替え実行時に Capture の NULL チェックを追加する
  - 接続直後にカメラ切り替えボタンを押下した際に NullPointerException が発生することがあったため
  - @t-miya
- [UPDATE] デバイス権限処理を PermissionDispacher から Activity Result API へ移行
  - kapt が Kotlin 2 に対応していないため警告が出て 1.9.0 にフォールバックされる
  - kapt を ksp へ移行しようとすると PermissionDispatcher が対応していないためビルドエラーとなる
  - PermissionDispatcher は 2022 年以降更新されておらずサードパーティー製ライブラリであるため Google 製の Activity Result API へ移行
  - @t-miya
- [UPDATE] Android の非推奨 API を後方互換しつつ移行する
  - ScreencastRequest
    - Parcel.readParcelable(ClassLoader?) → API 33 以降は readParcelable(ClassLoader, Class)、未満は非推奨 API を抑制して分岐
  - SoraScreencastService
    - Intent.getParcelableExtra(String) → API 33 以降は getParcelableExtra(String, Class)、未満は非推奨 API を抑制して分岐
  - MessagingActivity
    - Icons.Filled.Send → Icons.AutoMirrored.Filled.Send(import も automirrored に更新)
  - SimulcastActivity / VideoChatRoomActivity / VoiceChatRoomActivity
    - 画面フルスクリーン制御 `FLAG_FULLSCREEN` / `systemUiVisibility` → `WindowCompat` + `WindowInsetsControllerCompat` に置換
  - Handler() → Handler(Looper.getMainLooper()) に置換
  - SoraScreenUtil
    - 画面サイズ取得 `WindowManager.defaultDisplay.getSize` → API 30 以降は `currentWindowMetrics`、未満は `displayMetrics` に置換
  - @t-miya
- [UPDATE] Kotlin バージョンを 2.0.20 に上げる
  - @t-miya
- [UPDATE] Sora Android SDK を 2025.3.0 に上げる
  - @zztkm
- [UPDATE] samples/build.gradle.kts から不要な flavor 設定を削除する
  - 削除内容
    - `flavorDimensions.add("tier")` の設定を削除
    - `productFlavors { create("free") { versionNameSuffix = "-free" } }` のブロックを削除
  - 利用していない flavor でビルドバリアントが増えていたため、Quickstart アプリと同じ構成に揃えて運用を簡素化するため
  - @zztkm
- [ADD] オーディオ(マイク)のハードミュート機能を追加する
  - 実機のおいてマイクのインジケータが消灯する状態にできる
  - ビデオチャットサンプル、サイマルキャストサンプル、スポットライトサンプルのマイクミュートボタンにハードミュートに切り替える機能を追加する
    - 音声配信 -> ソフトミュート(audioトラック無効) -> ハードミュート(録音停止) -> 音声配信 -> ... で切り替わるボタンを追加する
    - MicMuteController クラスを追加し、VideoChatRoomActivity/SimulcastActivity での音声ミュート切り替え処理を共通化する
- [ADD] マイクのミュート制御の共通モジュールとして MicMuteController を追加した
  - 排他制御によりボタン連打等による不整合を防ぐ
  - ミュートボタンを扱うアクティビティで利用できる
  - @t-miya

### misc

- [UPDATE] ktlint バージョンを上げる
  - ktlint を 1.7.1 に上げる
  - ktlint-gradle を 13.1.0 に上げる
  - @t-miya
- [UPDATE] gradle を Kotlin DSL 対応する
  - build.gradle、settings.gradle、samples/build.gradle を kts ファイルに置き換えた
  - ライブラリバージョン管理を Version Catalog による管理に変更した
  - @t-miya
- [UPDATE] actions/checkout を v5 に上げる
  - @miosakuma
- [ADD] ローカルの sora-android-sdk を Composite build で取り込めるようにする
  - gradle.properties.example にローカルの sora-android-sdk のソースコードのパスを指定する `soraSdkDirPath` キーを追加
  - settings.gradle.kts に `soraSdkDirPath` のパスを Composite build で取り込む処理を追加
  - settings.gradle.kts の実装は Composite build 利用時のみコメントアウトする。デフォルトでは JitPack 経由での SDK 利用となる
  - @t-miya

## sora-andoroid-sdk-2025.2.0

**リリース日**: 2025-09-17

- [CHANGE] マルチストリーム設定を廃止する
  - レガシーストリーム機能は 2025 年 6 月リリースの Sora にて廃止されるため、サンプルアプリケーションでもマルチストリーム設定を廃止する
  - Sora がデフォルトでレガシーストリームを使用するように設定されている場合、接続エラーになる
  - @zztkm
- [CHANGE] ビデオチャットサンプル、サイマルキャストサンプル、スポットライトサンプルの「解像度の変更」項目の内容を fixedResolution から DegradationPreference に変更する
  - 2025.2.0 で解像度維持の設定が fixedResolution から DegradationPreference に変更したことに伴う対応
  - スポットライトサンプルは「解像度の変更」項目がないため、他のサンプルに合わせるために追加をした
  - @miosakuma
- [UPDATE] システム条件を更新する
  - Android Studio 2025.1.1 以降
  - WebRTC SFU Sora 2025.1.0 以降
  - @miosakuma @zztkm
- [UPDATE] 依存ライブラリーのバージョンを上げる
  - com.google.code.gson:gson を 2.13.1 に上げる
  - androidx.appcompat:appcompat を 1.7.1 に上げる
  - androidx.recyclerview:recyclerview を 1.4.0 に上げる
  - androidx.constraintlayout:constraintlayout を 2.2.1 に上げる
  - androidx.navigation:navigation-fragment-ktx を 2.9.3 に上げる
  - androidx.navigation:navigation-ui-ktx を 2.9.3 に上げる
  - androidx.compose.ui:ui を 1.8.3 に上げる
  - androidx.compose.material:material を 1.8.3 に上げる
  - androidx.compose.material:material-icons-extended を 1.7.8 に上げる
  - androidx.activity:activity-compose を 1.10.1 に上げる
  - com.android.tools.build:gradle を 8.11.1 に上げる
  - Gradle を 8.14.3 に上げる
  - compileSdkVersion を 36 に上げる
  - targetSdkVersion を 36 に上げる
  - @miosakuma
- [UPDATE] edge-to-edge の画面表示に対応する
  - targetSdkVersion 35 以降 edge-to-edge の画面表示がデフォルトとなった
  - 各画面レイアウト に `android:fitsSystemWindows="true"` を指定する
  - リアルタイムメッセージングサンプルのコンポーザブルに systemBarsPadding() を設定する
  - バックグラウンドカラーを白以外にしてステータスバーの文字が見えるようにする
  - @miosakuma
- [UPDATE] 権限に FOREGROUND_SERVICE_MEDIA_PROJECTION を追加する 
  - スクリーンキャストのサービスを起動するために必要な権限
  - Android 14 (API level 34) 以降で必要になる
  - @miosakuma
- [UPDATE] ビデオエフェクトサンプルを削除する
  - `jp.co.cyberagent.android:gpuimage:2.1.0` が 16 KB ページサイズに対応していないため
  - @miosakuma
- [UPDATE] Sora Android SDK を 2025.2.0 にあげる
  - onError(SoraMediaChannel, SoraErrorReason) の廃止に対応する
  - onClose(mediaChannel: SoraMediaChannel) から onClose(mediaChannel: SoraMediaChannel, closeEvent: SoraCloseEvent) へ移行する
  - CameraCapturerFactory.create() の引数から fixedResolution を削除する
  - @miosakuma
- [UPDATE] ビデオチャットサンプル、サイマルキャストサンプル、スポットライトサンプルの映像コーデックに `未指定` を追加する
  - @miosakuma
- [UPDATE] 映像コーデックリストの表示順序を統一する
  - @miosakuma
- [UPDATE] ビデオチャットサンプル、ボイスチャットサンプル、サイマルキャストサンプル、スポットライトサンプルの音声コーデックに `未指定` を追加する
  - @miosakuma
- [ADD] ビデオチャットサンプル、サイマルキャストサンプル、スポットライトサンプルの接続設定メニューに `開始時カメラ` を追加した
  - `無効` にして接続した場合、カメラはハードウェアミュート状態で開始される
  - @t-miya
- [ADD] ビデオチャットサンプル、サイマルキャストサンプル、スポットライトサンプルにカメラミュートボタンを追加した
  - 映像配信 -> ソフトウェアミュート(videoトラック無効) -> ハードウェアミュート(カメラキャプチャ停止) -> 映像配信 -> ... で切り替わるボタンを追加した
  - @t-miya

### misc

- [UPDATE] GitHub Actions の定期実行をやめる
  - @zztkm
- [ADD] .github ディレクトリに copilot-instructions.md を追加
  - @torikizi

## sora-andoroid-sdk-2025.1.1

**リリース日**: 2025-08-07

- [UPDATE] Sora Android SDK を 2025.1.1 にあげる
  - @miosakuma

## sora-andoroid-sdk-2025.1.0

**リリース日**: 2025-01-27

- [UPDATE] システム条件を更新する
  - Android Studio 2024.2.2 以降
  - @miosakuma @zztkm

## sora-andoroid-sdk-2024.3.1

**リリース日**: 2024-08-30

- [UPDATE] システム条件を更新する
  - Android Studio 2024.1.1 以降
  - WebRTC SFU Sora 2024.1.0 以降
  - Sora Android SDK 2024.3.1 以降
  - @miosakuma
- [UPDATE] Android Gradle Plugin (AGP) を 8.5.0 にアップグレードする
  - Android Studion の AGP Upgrade Assistant を利用してアップグレードされた内容
    - `com.android.tools.build:gradle` を 8.5.0 に上げる
    - ビルドに利用される Gradle を 8.7 に上げる
    - Android マニフェストからビルドファイルにパッケージを移動
      - Android マニフェストに定義されていた package を削除
      - ビルドファイルに namespace を追加
    - ビルドファイルの dependencies の transitive をコメントアウト
  - AGP 8.5.0 対応で発生したビルドスクリプトのエラーを手動で修正した内容
    - AGP 8.0 から buildConfig がデフォルト false になったため、true に設定する
  - @zztkm
- [UPDATE] 依存ライブラリーのバージョンを上げる
  - com.google.code.gson:gson を 2.11.0 に上げる
  - androidx.appcompat:appcompat を 1.7.0 に上げる
  - androidx.recyclerview:recyclerview を 1.3.2 に上げる
  - com.google.android.material:material を 1.12.0 に上げる
  - androidx.navigation:navigation-fragment-ktx を 2.7.7 に上げる
  - androidx.navigation:navigation-ui-ktx を 2.7.7 に上げる
  - androidx.compose.ui:ui を 1.6.8 に上げる
  - androidx.compose.material:material を 1.6.8 に上げる
  - androidx.compose.material:material-icons-extended を 1.6.8 に上げる
  - androidx.activity:activity-compose を 1.9.1 に上げる
  - @zztkm
- [UPDATE] compileSdkVersion を 34 に上げる
  - Android API レベル 34 以降でコンパイルする必要がある依存ライブラリがあるため
  - @zztkm
- [UPDATE] Kotlin のバージョンを 1.9.25 に上げる
  - 合わせて、kotlinCompilerExtensionVersion を 1.5.15 に上げる
  - @zztkm
- [UPDATE] スクリーンキャストサンプルで 1 つのアプリを選択して配信した際の挙動を改善する
  - スクリーンキャストの映像を送信するために、MainActivity に画面更新を促す Intent を送る処理を追加
    - スクリーンキャストサンプルは画面内に動きがなく、画面を動かすまで映像が送信されない問題があったため
  - `Could not create virtual display` というエラーが出て 1 つのアプリでスクリーンキャストできない問題を修正
    -  SoraScreencastService を起動する Activity タスクを分けることで回避できることがわかったため、`ScreencastSetupActivity` の launchMode を `singleInstance` に変更した
    - Activity のタスクを分けることに合わせて画面遷移の見直しを行った
  - 動作確認は Android 14 の Pixel 端末でのみ行っており、他の端末での動作は未確認
  - @tnoho
- [FIX] `Handler ()` で現在のスレッドに関連付けられた Looper を利用するようになっていたことで発生していた以下の問題を修正する
  - 発生した問題
    - スクリーンキャストが正常終了しなかった場合に、`SoraScreencastService.closeChannel()` の処理が main スレッド以外で実行されて `CalledFromWrongThreadException` が発生する
    - `SoraMediaChannel.Listener.onClose()` の呼び出しにより `SoraScreencastService.closeChannel()` を実行するときに内部の `Handler()` 呼び出しがブロッキングされ、アプリが停止するケースがあった
  - 修正内容
   - `Handler (Looper looper)` に `getMainLooper()` で取得した、メインスレッドに関連付けられた Looper を利用するようにした
  - @tnoho

## sora-andoroid-sdk-2024.3.0

Sora Android SDK 2024.3.0 のリリース作業時に発生した問題によりスキップしました。

## sora-andoroid-sdk-2024.2.0

- [UPDATE] システム条件を更新する
  - Sora Android SDK 2024.2.0 以降
  - @miosakuma
- [UPDATE] Github Actions の actions/setup-java@v4 にあげる
  - @miosakuma
- [FIX] Github Actions でのビルドを Java 17 にする
  - @miosakuma

## sora-andoroid-sdk-2024.1.1

- [UPDATE] システム条件を更新する
  - Android Studio 2023.2.1 以降
  - WebRTC SFU Sora 2023.2.0 以降
  - Sora Android SDK 2024.1.1 以降
  - @miosakuma
- [UPDATE] ビデオチャット、サイマルキャスト、スポットライトのサンプルを H.265 に対応する
  - @enm10k
- [UPDATE] 解像度に qHD (960x540, 540x960) を追加する
  - @enm10k
- [FIX] SoraFrameSize.portrait のキーで幅と高さが逆になっているものがあったので修正
  - @enm10k
- [FIX] マルチウィンドウモード時に画面を回転すると Activity が再作成されるのを防ぐ設定を入れる
  - @miosakuma

## sora-andoroid-sdk-2024.1.0

Sora Android SDK 2024.1.0 のリリース作業時に発生した問題によりスキップしました。

## sora-andoroid-sdk-2023.2.0

- [UPDATE] システム条件を更新する
  - Android Studio 2022.2.1 以降
  - WebRTC SFU Sora 2023.1.0 以降
  - Sora Android SDK 2023.2.0 以降
  - @miosakuma
- [ADD] ビデオチャットにサンプルに映像コーデックのプロファイルを追加する
  - @miosakuma
- [ADD] サイマルキャストサンプルの映像コーデックに VP9 と AV1 を追加する
  - @szktty

## sora-andoroid-sdk-2023.1.0

- [UPDATE] システム条件を更新する
  - Android Studio 2022.1.1 以降
  - WebRTC SFU Sora 2022.2.0 以降
  - Sora Android SDK 2023.1.0 以降
  - @miosakuma
- [UPDATE] `compileSdkVersion` を 33 に上げる
  - @miosakuma
- [UPDATE] `targetSdkVersion` を 33 に上げる
  - @miosakuma
- [UPDATE] Kotlin のバージョンを 1.8.10 に上げる
  - @miosakuma
- [UPDATE] Compose Compiler のバージョンを 1.4.3 に上げる
  - @miosakuma
- [UPDATE] Gradle を 7.6.1 に上げる
  - @miosakuma
- [UPDATE] 依存ライブラリーのバージョンを上げる

  - com.android.tools.build:gradle を 7.4.2 に上げる
  - com.github.ben-manes:gradle-versions-plugin を 0.46.0 に上げる
  - org.jlleitschuh.gradle:ktlint-gradle を 11.3.1 に上げる
  - com.google.code.gson:gson を 2.10.1 に上げる
  - androidx.appcompat:appcompat を 1.6.1 に上げる
  - androidx.recyclerview:recyclerview: を 1.3.0 に上げる
  - com.google.android.material:material: を 1.8.0 に上げる
  - androidx.navigation:navigation-fragment-ktx を 2.5.3 に上げる
  - androidx.navigation:navigation-ui-ktx を 2.5.3 に上げる
  - androidx.compose.ui:ui:1.4.0 に上げる
  - androidx.compose.material:material を 1.4.0 に上げる
  - androidx.compose.material:material-icons-extended を 1.4.0 に上げる
  - androidx.activity:activity-compose を 1.7.0 に上げる

- [ADD] 映像コーデックに AV1 を追加する
  - @miosakuma
- [ADD] ビデオチャットサンプルに音声ストリーミング機能の言語コードを追加する
  - @miosakuma

## sora-andoroid-sdk-2022.4.0

- [UPDATE] `compileSdkVersion` を 32 に上げる
  - @miosakuma
- [UPDATE] `targetSdkVersion` を 32 に上げる
  - @miosakuma
- [UPDATE] Kotlin のバージョンを 1.7.10 に上げる
  - @miosakuma
- [UPDATE] Gradle を 7.5.1 に上げる
  - @miosakuma
- [UPDATE] 依存ライブラリーのバージョンを上げる
  - com.google.code.gson:gson を 2.9.1 に上げる
  - androidx.appcompat:appcompat を 1.5.0 に上げる
  - androidx.navigation:navigation-fragment-ktx を 2.5.1 に上げる
  - androidx.navigation:navigation-ui-ktx を 2.5.1 に上げる
  - androidx.compose.ui:ui:1.2.1 に上げる
  - androidx.compose.material:material を 1.2.1 に上げる
  - androidx.compose.material:material-icons-extended を 1.2.1 に上げる
  - androidx.activity:activity-compose を 1.5.1 に上げる
  - com.android.tools.build:gradle を 7.2.2 に上げる
  - @miosakuma

## sora-andoroid-sdk-2022.3.0

- [ADD] 解像度の調整を ON/OFF する UI を追加する
  - @enm10k
- [ADD] プロキシを gradle.properties ファイルから設定できるようにする
  - @enm10k
- [UPDATE] システム条件を更新する
  - Android Studio 2021.2.1 以降
  - WebRTC SFU Sora 2022.1.0 以降
  - Sora Android SDK 2022.3.0 以降
  - @miosakuma
- [UPDATE] compileSdkVersion を 31 に上げる
  - AndroidManifest.xml の Activity に `android:exported="true"` を明示的に記載する
  - @miosakuma
- [UPDATE] Gradle のバージョンを 7.4.2 に上げる
  - @miosakuma
- [UPDATE] Gktlint のバージョンを 0.45.2 に上げる
  - @miosakuma
- [UPDATE] 依存ライブラリを更新する
  - com.android.tools.build:gradle を 7.2.1 に上げる
  - org.jlleitschuh.gradle:ktlint-gradle を 10.3.0 に上げる
  - com.google.code.gson:gson を 2.9.0 に上げる
  - androidx.appcompat:appcompat を 1.4.2 に上げる
  - com.google.android.material:material を 1.6.1 に上げる
  - androidx.constraintlayout:constraintlayout を 2.1.4 に上げる
  - androidx.navigation:navigation-fragment-ktx を 2.4.2 に上げる
  - androidx.navigation:navigation-ui-ktx を 2.4.2 に上げる
  - androidx.compose.ui:ui:1.1.1 に上げる
  - androidx.compose.material:material を 1.1.1 に上げる
  - androidx.compose.material:material-icons-extended を 1.1.1 に上げる
  - androidx.activity:activity-compose を 1.4.0 に上げる
  - com.github.permissions-dispatcher:permissionsdispatcher を 4.9.2 　に上げる
  - com.github.permissions-dispatcher:permissionsdispatcher-processor を 4.9.2 　に上げる
  - com.github.ben-manes:gradle-versions-plugin を 0.42.0 に上げる
  - @miosakuma
- [FIX] メッセージングアプリが H.264 で接続中の別クライアントがいるときに接続エラーになる問題を修正する
  - @miosakuma

## sora-andoroid-sdk-2022.2.0

- [UPDATE] システム条件を更新する
  - Android 8.0 以降
  - Android Studio 2022.1.1 以降
  - Sora Android SDK 2022.2.0 以降
  - @miosakuma
- [UPDATE] Kotlin synthetics の廃止に伴い View binding に移行する
  - @miosakuma @enm10k
- [CHANGE] minSdkVersion を 26 に上げる
  - @enm10k

## sora-andoroid-sdk-2022.1.0

- [UPDATE] システム条件を更新する
  - Android Studio 2020.3.1 以降
  - Sora Android SDK 2022.1.0 以降
  - @miosakuma
- [ADD] シグナリング接続時に送信するメタデータを外部ファイルから設定できるようにする
  - @miosakuma
- [CHANGE] スポットライトレガシーを削除する
  - @enm10k
- [CHANGE] シグナリングの URL 指定を `signaling_endpoint` から `signalingEndpointCandidates` に変更する
  - @miosakuma @enm10k
- [FIX] sendrecv 接続時に映像の送信を無効に設定し、かつ相手が H.264 の映像を送信するとき接続が失敗する不具合を修正する
  - @miosakuma

## sora-android-sdk-2021.3

- [UPDATE] システム条件を更新する
  - Sora Android SDK 2021.3 以降
  - @miosakuma

## sora-android-sdk-2021.2

- [UPDATE] システム条件を更新する
  - Android Studio 4.2 以降
  - WebRTC SFU Sora 2021.1 以降
  - Sora Android SDK 2021.2 以降
  - @miosakuma
- [UPDATE] sdk が channel_id を必須にした変更に追従する
  - @shino
- [UPDATE] spotlight_number は未指定をデフォルトにする
  - @shino
- [UPDATE] `com.android.tools.build:gradle` を 4.2.2 に上げる
  - @enm10k
- [UPDATE] JCenter への参照を取り除く
  - @enm10k
- [UPDATE] シグナリングエンドポイント URL の設定を `/build.gradle` から `/gradle.properties.example` に移動する
  - @miosakuma
- [ADD] サイマルキャストの接続時に simulcast_rid を指定できるようにする
  - @enm10k
- [ADD] スポットライトの接続時に spotlight_focus_rid / spotlight_unfocus_rid を指定できるようにする
  - @enm10k
- [CHANGE] サイマルキャスト画面から、受信する rid を指定するボタンを削除する
  - @enm10k
- [FIX] 自身の映像プレビューが反転している問題を修正する
  - @torikizi
- [FIX] gradle.properties で指定した usesCleartextTraffic が参照されずに、常に true になっていた問題を修正する
  - @enm10k

## sora-android-sdk-2021.1.1

- [UPDATE] SDK のバージョンを 2021.1.1 に上げる
  - @enm10k

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
  - 参考: <https://developer.android.com/about/versions/10/features?hl=ja#fg-service-types>
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
    video が false のときには video のトラックが含まれなかった - Plan B の制限による挙動だった

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

  - cf. <https://webrtc-review.googlesource.com/c/src/+/43022>

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
- コマンドラインビルドのエラー回避のため、 org.jetbrains:annotations を compile 依存から除外する

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
