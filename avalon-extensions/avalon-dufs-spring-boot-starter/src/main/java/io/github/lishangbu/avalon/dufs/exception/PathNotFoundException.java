package io.github.lishangbu.avalon.dufs.exception;

/**
 * 路径找不到
 *
 * @author lishangbu
 * @since 2025/8/11
 */
public class PathNotFoundException extends DufsException {
  /**
   * 带有错误信息的构造函数，创建一个新的 PathNotFoundException 异常实例。
   *
   * @param message 异常信息
   */
  public PathNotFoundException(String message) {
    super(message);
  }
}
