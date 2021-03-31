package jp.shiguredo.sora.sample.option

enum class SoraRoleType {
    SENDONLY {
        override fun hasDownstream() = false
        override fun hasUpstream() = true
    },
    RECVONLY {
        override fun hasDownstream() = true
        override fun hasUpstream() = false
    },
    SENDRECV {
        override fun hasDownstream() = true
        override fun hasUpstream() = true
    };
    abstract fun hasUpstream(): Boolean
    abstract fun hasDownstream(): Boolean
}
