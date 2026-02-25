package io.github.lishangbu.avalon.ip2location.exception;

/// IP 数据库文件缺失异常
///
/// 当指定的 IP 数据库文件缺失或路径无效时抛出此异常
///
/// @author lishangbu
/// @since 2025/4/12
public class MissingFileException extends IpToLocationException {

    /// 构造一个新的 MissingFileException 异常，并设置默认错误信息
    /// 默认错误信息为: "Invalid database path."
    public MissingFileException() {
        super("Invalid database path.");
    }
}
