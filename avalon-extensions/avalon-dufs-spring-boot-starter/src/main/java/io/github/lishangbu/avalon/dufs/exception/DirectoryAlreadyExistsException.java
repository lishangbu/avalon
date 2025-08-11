package io.github.lishangbu.avalon.dufs.exception;

/**
 * 文件夹已经存在
 *
 * @author lishangbu
 * @since 2025/8/11
 */
public class DirectoryAlreadyExistsException extends DufsException {
  /**
   * 带有错误信息的构造函数，创建一个新的 DirectoryAlreadyExistsException 异常实例。
   *
   * @param message 异常信息
   */
  public DirectoryAlreadyExistsException(String message) {
    super(message);
  }
}
