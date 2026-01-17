package io.github.lishangbu.avalon.ip2location.exception;

/// 空 IP 地址异常
///
/// 当传入的 IP 地址为空或仅包含空白字符时抛出
///
/// @author lishangbu
/// @since 2025/4/12
public class EmptyIpAddressException extends IpToLocationException {
  /// 构造一个新的空 IP 地址异常，并设置默认错误信息
  public EmptyIpAddressException() {
    super("IP address cannot be blank.");
  }
}
