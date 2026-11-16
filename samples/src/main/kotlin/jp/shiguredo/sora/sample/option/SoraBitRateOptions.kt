package jp.shiguredo.sora.sample.option

/**
 * サンプルの接続設定画面で選択できる映像ビットレートと音声ビットレート
 *
 * Sora DevTools の `src/constants.ts` にある `VIDEO_BIT_RATES` / `AUDIO_BIT_RATES` と
 * 同じ値・同じ並びにしている。Sora DevTools 側を変更する場合はこちらも合わせて更新すること。
 * 値の単位は kbps で、`未指定` を除く要素は各画面で数値に変換して SDK に渡す。
 * 先頭の要素は `未指定` であり、`defaultIndex` を省略した画面では先頭が初期選択になる。
 *
 * 参照: <https://github.com/shiguredo/sora-devtools/blob/develop/src/constants.ts>
 */
object SoraBitRateOptions {
    val video: List<String> =
        listOf(
            "未指定",
            "10",
            "30",
            "50",
            "100",
            "300",
            "500",
            "800",
            "1000",
            "1500",
            "2000",
            "2500",
            "3000",
            "5000",
            "10000",
            "15000",
            "20000",
            "30000",
            "50000",
        )

    val audio: List<String> =
        listOf(
            "未指定",
            "8",
            "16",
            "24",
            "32",
            "64",
            "96",
            "128",
            "256",
            "384",
        )
}
