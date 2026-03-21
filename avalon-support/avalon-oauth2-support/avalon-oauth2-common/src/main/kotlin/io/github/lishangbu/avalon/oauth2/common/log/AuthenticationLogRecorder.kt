package io.github.lishangbu.avalon.oauth2.common.log

fun interface AuthenticationLogRecorder {
    fun record(record: AuthenticationLogRecord)

    companion object {
        @JvmStatic
        fun noop(): AuthenticationLogRecorder = AuthenticationLogRecorder {}
    }
}
