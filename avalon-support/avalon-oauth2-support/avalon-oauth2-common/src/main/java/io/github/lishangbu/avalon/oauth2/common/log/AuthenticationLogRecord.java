package io.github.lishangbu.avalon.oauth2.common.log;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record AuthenticationLogRecord(
        @Nullable String username,
        @Nullable String clientId,
        @Nullable String grantType,
        @Nullable String ip,
        @Nullable String userAgent,
        boolean success,
        @Nullable String errorMessage,
        @Nullable Instant timestamp) {}
