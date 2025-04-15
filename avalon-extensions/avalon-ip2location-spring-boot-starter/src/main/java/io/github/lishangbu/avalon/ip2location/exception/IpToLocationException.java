package io.github.lishangbu.avalon.ip2location.exception;

/**
 * 自定义异常类，用于处理IP到位置转换过程中发生的异常。
 *
 * @author lishangbu
 * @since 2025/4/15
 */
public class IpToLocationException extends RuntimeException {

  /** 无参构造函数，创建一个新的 `IpToLocationException` 异常实例。 */
  public IpToLocationException() {}

  /**
   * 带有错误信息的构造函数，创建一个新的 `IpToLocationException` 异常实例。
   *
   * @param message 异常信息
   */
  public IpToLocationException(String message) {
    super(message);
  }
}
