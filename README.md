# Sora Android SDK サンプルアプリケーション

このリポジトリは [Sora Android SDK](https://github.com/shiguredo/sora-android-sdk) を利用したサンプルアプリケーションを提供しています。Sora Android SDK の使い方は [Sora Android SDK ドキュメント](https://sora-android-sdk.shiguredo.jp/)を参照してください。

## About Shiguredo's open source software

We will not respond to PRs or issues that have not been discussed on Discord. Also, Discord is only available in Japanese.

Please read https://github.com/shiguredo/oss before use.

## 時雨堂のオープンソースソフトウェアについて

利用前に https://github.com/shiguredo/oss をお読みください。

## システム条件

- Android 8.0 以降 (エミュレーターでの動作は保証しません)
- Android Studio 2025.3.1 以降
- WebRTC SFU Sora 2025.2.0 以降

## サンプルの使い方

最新 [Releases](https://github.com/shiguredo/sora-android-sdk-samples/releases) をダウンロードしてください。

## サンプルの紹介

### ビデオチャット

ビデオチャットのデモです。複数人でのグループチャットも可能です。

### ボイスチャット

ボイスチャットのデモです。複数人でのグループチャットも可能です。

### サイマルキャスト

サイマルキャストのデモです。

### スポットライト

スポットライトのデモです。アクティブ配信数を固定したチャットが可能です。

### RPC チャット

RPC 機能のデモです。

### リアルタイムメッセージング

リアルタイムメッセージングのデモです。

### スクリーンキャスト

クライアント端末の画面を動画配信するデモです。配信を開始すると箱積みゲームの画面が表示され、ゲームのプレイ内容を配信します。
画面をタップすると箱が落下して積み上がります。箱は物理演算で動くため、積み上げた箱に新しい箱がぶつかると崩れることがあります。

ゲーム画面のみを配信するには、画面共有の確認画面でアプリ単位の共有を選択できる場合は、アプリの一覧から「Sora Android SDK サンプル集」を選択してください。選択できない場合は、画面全体を共有してください。画面共有の確認画面の文言は Android のバージョンによって異なります。

配信を停止するには、配信中に表示されるオーバーレイの停止ボタン (×) をタップします。ゲーム画面で戻る操作をした場合はゲーム画面だけが終了し、配信は継続します。その場合は通知またはオーバーレイの起動ボタンからゲーム画面に戻れます。

## Copyright

- Copyright 2023, Shiguredo Inc. and Lyo Kato <lyo.kato at gmail.com>
