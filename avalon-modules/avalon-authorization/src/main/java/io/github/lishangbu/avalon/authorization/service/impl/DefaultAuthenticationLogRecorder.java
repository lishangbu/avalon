package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.entity.AuthenticationLog;
import io.github.lishangbu.avalon.authorization.repository.AuthenticationLogRepository;
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecord;
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultAuthenticationLogRecorder implements AuthenticationLogRecorder {

    private final AuthenticationLogRepository authenticationLogRepository;

    @Override
    public void record(AuthenticationLogRecord record) {
        if (record == null) {
            return;
        }
        AuthenticationLog log = new AuthenticationLog();
        log.setUsername(record.username());
        log.setClientId(record.clientId());
        log.setGrantType(record.grantType());
        log.setIp(record.ip());
        log.setUserAgent(record.userAgent());
        log.setSuccess(record.success());
        log.setErrorMessage(record.errorMessage());
        log.setOccurredAt(record.timestamp() == null ? Instant.now() : record.timestamp());
        authenticationLogRepository.save(log);
    }
}
