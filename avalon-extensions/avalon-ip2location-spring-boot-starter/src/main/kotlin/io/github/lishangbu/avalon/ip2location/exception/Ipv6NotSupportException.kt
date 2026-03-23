package io.github.lishangbu.avalon.ip2location.exception

/**
 * IPv6 不受支持异常
 *
 * 当数据库文件中不包含 IPv6 数据时抛出
 */
class Ipv6NotSupportException : IpToLocationException("This BIN does not contain IPv6 data.")
