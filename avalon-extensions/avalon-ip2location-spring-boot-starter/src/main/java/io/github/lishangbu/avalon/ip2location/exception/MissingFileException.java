package io.github.lishangbu.avalon.ip2location.exception;

/**
 * 空IP地址异常
 *
 * @author lishangbu
 * @since 2025/4/12
 */
public class MissingFileException extends IpToLocationException {
  public MissingFileException() {
    super("Invalid database path.");
  }
}
