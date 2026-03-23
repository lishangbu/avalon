package io.github.lishangbu.avalon.ip2location.exception

/**
 * 空 IP 地址异常
 *
 * 当传入的 IP 地址为空或仅包含空白字符时抛出
 */
class EmptyIpAddressException : IpToLocationException("IP address cannot be blank.")
