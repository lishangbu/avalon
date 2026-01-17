package io.github.lishangbu.avalon.dufs.exception;

/// 路径找不到异常
///
/// 在删除或访问资源时未找到指定路径时抛出
///
/// @author lishangbu
/// @since 2025/8/11
public class PathNotFoundException extends DufsException {
  /// 带有错误信息的构造函数
  ///
  /// @param message 异常信息
  public PathNotFoundException(String message) {
    super(message);
  }
}
