package io.github.lishangbu.avalon.ip2location.exception;

/**
 * 无效IP地址异常类
 *
 * @author lishangbu
 * @since 2025/4/12
 */
public class InvalidIpAddressException extends IpToLocationException {
  /** 构造一个新的无效IP地址异常，并设置默认错误信息 */
  public InvalidIpAddressException() {
    super("Invalid IP address.");
  }
}
