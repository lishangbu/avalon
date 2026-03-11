package io.github.lishangbu.avalon.oauth2.common.log;

@FunctionalInterface
public interface AuthenticationLogRecorder {

    void record(AuthenticationLogRecord record);

    static AuthenticationLogRecorder noop() {
        return record -> {};
    }
}
