package io.github.lishangbu.avalon.ip2location.exception;

/**
 * IP数据库文件不支持IPV6异常
 *
 * @author lishangbu
 * @since 2025/4/12
 */
public class Ipv6NotSupportException extends RuntimeException {
  /** 构造一个新的Ipv6NotSupportException异常，并设置默认错误信息。 默认错误信息为："This BIN does not contain IPv6 data."。 */
  public Ipv6NotSupportException() {
    super("This BIN does not contain IPv6 data.");
  }
}
