package io.github.lishangbu.avalon.ip2location.util

import net.renfei.ip2location.IPTools
import java.math.BigInteger
import java.net.UnknownHostException

/**
 * IP 工具类
 *
 * 封装 IPTools 提供的地址识别与转换能力
 */
object IpUtils {
    /** IPTools 实例 */
    private val ipTools: IPTools by lazy { IPTools() }

    /** 判断是否为 IPv4 地址 */
    fun isIpv4(ipAddress: String): Boolean = ipTools.IsIPv4(ipAddress)

    /** 判断是否为 IPv6 地址 */
    fun isIpv6(ipAddress: String): Boolean = ipTools.IsIPv6(ipAddress)

    /** 将 IPv4 地址转换为十进制 */
    fun ipv4ToDecimal(ipAddress: String): BigInteger? = ipTools.IPv4ToDecimal(ipAddress)

    /** 将 IPv6 地址转换为十进制 */
    fun ipv6ToDecimal(ipAddress: String): BigInteger? = ipTools.IPv6ToDecimal(ipAddress)

    /** 将十进制值转换为 IPv4 地址 */
    @Throws(UnknownHostException::class)
    fun decimalToIpv4(ipNumber: BigInteger): String = ipTools.DecimalToIPv4(ipNumber)

    /** 将十进制值转换为 IPv6 地址 */
    @Throws(UnknownHostException::class)
    fun decimalToIpv6(ipNumber: BigInteger): String = ipTools.DecimalToIPv6(ipNumber)

    /** 压缩 IPv6 地址 */
    fun compressIpv6(ipAddress: String): String = ipTools.CompressIPv6(ipAddress)

    /** 展开 IPv6 地址 */
    fun expandIpv6(ipAddress: String): String = ipTools.ExpandIPv6(ipAddress)

    /** 将 IPv4 地址范围转换为 CIDR 列表 */
    @Throws(UnknownHostException::class)
    fun ipv4ToCidr(
        ipFrom: String,
        ipTo: String,
    ): List<String> = ipTools.IPv4ToCIDR(ipFrom, ipTo)

    /** 将 IPv6 地址范围转换为 CIDR 列表 */
    @Throws(UnknownHostException::class)
    fun ipv6ToCidr(
        ipFrom: String,
        ipTo: String,
    ): List<String> = ipTools.IPv6ToCIDR(ipFrom, ipTo)

    /** 将 CIDR 转换为 IPv4 地址范围 */
    @Throws(UnknownHostException::class)
    fun cidrToIpv4(cidr: String): Array<String> = ipTools.CIDRToIPv4(cidr)

    /** 将 CIDR 转换为 IPv6 地址范围 */
    @Throws(UnknownHostException::class)
    fun cidrToIpv6(cidr: String): Array<String> = ipTools.CIDRToIPv6(cidr)
}
