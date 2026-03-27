package io.github.lishangbu.avalon.oauth2.common.log

fun interface AuthenticationLogRecorder {
    /** 记录信息 */
    fun record(record: AuthenticationLogRecord)

    companion object {
        /** 创建空操作实现 */
        @JvmStatic
        fun noop(): AuthenticationLogRecorder = AuthenticationLogRecorder {}
    }
}
