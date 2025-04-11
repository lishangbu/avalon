package io.github.lishangbu.avalon.security.exception;

import lombok.experimental.StandardException;

/**
 * 找不到JWT异常
 *
 * @author lishangbu
 * @since 2025/4/10
 */
@StandardException
public class JsonWebTokenNotFoundException extends RuntimeException {}
