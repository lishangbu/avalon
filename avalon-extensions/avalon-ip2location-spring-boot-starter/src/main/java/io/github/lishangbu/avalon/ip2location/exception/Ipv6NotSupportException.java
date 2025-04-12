package io.github.lishangbu.avalon.ip2location.exception;

/**
 * IpToLocation顶层异常处理
 *
 * @author lishangbu
 * @since 2025/4/12
 */
public class Ipv6NotSupportException extends RuntimeException {
  public Ipv6NotSupportException() {
    super("This BIN does not contain IPv6 data.");
  }
}
