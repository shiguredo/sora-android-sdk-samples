package jp.shiguredo.sora.sample.option

enum class SoraStreamType {
    SINGLE_UP {
        override fun hasDownstream() = false
        override fun hasUpstream() = true
        override fun hasMultistream() = false
    },
    SINGLE_DOWN {
        override fun hasDownstream() = true
        override fun hasUpstream() = false
        override fun hasMultistream() = false

    },
    MULTI_DOWN {
        override fun hasDownstream() = true
        override fun hasUpstream() = false
        override fun hasMultistream() = true

    },
    BIDIRECTIONAL {
        override fun hasDownstream() = true
        override fun hasUpstream() = true
        override fun hasMultistream() = true
    };
    abstract fun hasUpstream(): Boolean
    abstract fun hasDownstream(): Boolean
    abstract fun hasMultistream(): Boolean
}
