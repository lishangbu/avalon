package io.github.lishangbu.avalon.ip2location.exception

/**
 * IP 数据库文件缺失异常
 *
 * 当数据库文件缺失或路径无效时抛出
 */
class MissingFileException : IpToLocationException("Invalid database path.")
