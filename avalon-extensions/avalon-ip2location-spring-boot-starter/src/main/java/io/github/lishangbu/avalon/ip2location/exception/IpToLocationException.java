package io.github.lishangbu.avalon.ip2location.exception;

/// 自定义异常：IP 转换到位置信息时使用的通用异常类型
///
/// @author lishangbu
/// @since 2025/4/15
public class IpToLocationException extends RuntimeException {

    /// 无参构造函数，创建一个新的 IpToLocationException 实例
    public IpToLocationException() {}

    /// 带错误信息的构造函数
    ///
    /// @param message 异常信息
    public IpToLocationException(String message) {
        super(message);
    }
}
