package io.github.lishangbu.avalon.ip2location.exception

/**
 * 无效 IP 地址异常
 *
 * 当传入的 IP 地址格式非法时抛出
 */
class InvalidIpAddressException : IpToLocationException("Invalid IP address.")
