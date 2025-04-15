package io.github.lishangbu.avalon.ip2location.exception;

/**
 * 空IP地址异常
 *
 * @author lishangbu
 * @since 2025/4/12
 */
public class EmptyIpAddressException extends IpToLocationException {
  /** 构造一个新的空IP地址异常，并设置默认错误信息。 */
  public EmptyIpAddressException() {
    super("IP address cannot be blank.");
  }
}
