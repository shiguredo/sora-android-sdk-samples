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

- SDK のバージョンを上げた
- Kotlin を 1.1.50 に上げた
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
