package io.github.lishangbu.avalon.ip2location.exception;

/**
 * 非法IP地址异常
 *
 * @author lishangbu
 * @since 2025/4/12
 */
public class InvalidIpAddressException extends IpToLocationException {
  public InvalidIpAddressException() {
    super("Invalid IP address.");
  }
}
