package io.github.lishangbu.avalon.dufs.exception;

/// 文件夹已经存在异常
///
/// 在尝试创建已存在的目录时抛出
///
/// @author lishangbu
/// @since 2025/8/11
public class DirectoryAlreadyExistsException extends DufsException {
  /// 带有错误信息的构造函数
  ///
  /// @param message 异常信息
  public DirectoryAlreadyExistsException(String message) {
    super(message);
  }
}
