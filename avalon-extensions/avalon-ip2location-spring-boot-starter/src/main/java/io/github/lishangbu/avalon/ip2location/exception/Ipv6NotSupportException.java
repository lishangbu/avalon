package io.github.lishangbu.avalon.ip2location.exception;

/// IP 数据库文件不支持 IPv6 的异常
///
/// 当数据库文件中不包含 IPv6 数据时抛出
///
/// @author lishangbu
/// @since 2025/4/12
public class Ipv6NotSupportException extends RuntimeException {
    /// 构造新的 Ipv6NotSupportException，并设置默认错误信息
    /// 默认错误信息为："This BIN does not contain IPv6 data."。
    public Ipv6NotSupportException() {
        super("This BIN does not contain IPv6 data.");
    }
}
