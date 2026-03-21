package io.github.lishangbu.avalon.ip2location.util

import net.renfei.ip2location.IPTools
import java.math.BigInteger
import java.net.UnknownHostException

/**
 * IP 工具类 提供 IP 地址格式识别与相互转换的工具方法，封装第三方 IPTools 功能
 *
 * @param ipAddress 要检查的 IP 地址
 * @param ipAddress 要转换的 IPv4 地址
 * @param ipAddress 要转换的 IPv6 地址
 * @param ipNumber 要转换的十进制 IP 值
 * @param ipAddress 要压缩的 IPv6 地址
 * @param ipAddress 要展开的 IPv6 地址
 * @param ipFrom 范围起始 IPv4 地址
 * @param ipTo 范围结束 IPv4 地址
 * @param ipFrom 范围起始 IPv6 地址
 * @param ipTo 范围结束 IPv6 地址 将 CIDR 表示的 IPv4 转换为起止地址数组
 * @param CIDR 要转换的 CIDR 表示法
 * @return 如果是 IPv4 则返回 `true`，否则返回 `false` 判断给定字符串是否为 IPv6 地址
 * @return 如果是 IPv6 则返回 `true`，否则返回 `false` 将 IPv4 地址转换为十进制表示
 * @return 转换后的十进制 [BigInteger] 将 IPv6 地址转换为十进制表示
 * @return 转换后的十进制 [BigInteger] 将十进制表示转换为 IPv4 地址字符串
 * @return 转换后的 IPv4 地址字符串
 * @return 转换后的 IPv6 地址字符串 将 IPv6 地址压缩为简短形式
 * @return 压缩后的 IPv6 字符串 将 IPv6 地址展开为完整形式
 * @return 展开后的 IPv6 字符串 将 IPv4 范围转换为 CIDR 表示法列表
 * @return CIDR 字符串列表 将 IPv6 范围转换为 CIDR 表示法列表
 * @return 包含起始和结束 IPv4 的字符串数组 将 CIDR 表示的 IPv6 转换为起止地址数组
 * @return 包含起始和结束 IPv6 的字符串数组
 * @throws UnknownHostException 当无法将字节数组转换为 IP 地址时抛出 将十进制表示转换为 IPv6 地址字符串
 * @author lishangbu
 * @since 2025/4/12 判断给定字符串是否为 IPv4 地址
 */

/**
 * IP 工具类。
 *
 * 提供 IP 地址格式识别与相互转换的工具方法，封装第三方 IPTools 功能。
 */
object IpUtils {
    private val ipTools: IPTools by lazy { IPTools() }

    fun isIpv4(ipAddress: String): Boolean = ipTools.IsIPv4(ipAddress)

    fun isIpv6(ipAddress: String): Boolean = ipTools.IsIPv6(ipAddress)

    fun ipv4ToDecimal(ipAddress: String): BigInteger? = ipTools.IPv4ToDecimal(ipAddress)

    fun ipv6ToDecimal(ipAddress: String): BigInteger? = ipTools.IPv6ToDecimal(ipAddress)

    @Throws(UnknownHostException::class)
    fun decimalToIpv4(ipNumber: BigInteger): String = ipTools.DecimalToIPv4(ipNumber)

    @Throws(UnknownHostException::class)
    fun decimalToIpv6(ipNumber: BigInteger): String = ipTools.DecimalToIPv6(ipNumber)

    fun compressIpv6(ipAddress: String): String = ipTools.CompressIPv6(ipAddress)

    fun expandIpv6(ipAddress: String): String = ipTools.ExpandIPv6(ipAddress)

    @Throws(UnknownHostException::class)
    fun ipv4ToCidr(
        ipFrom: String,
        ipTo: String,
    ): List<String> = ipTools.IPv4ToCIDR(ipFrom, ipTo)

    @Throws(UnknownHostException::class)
    fun ipv6ToCidr(
        ipFrom: String,
        ipTo: String,
    ): List<String> = ipTools.IPv6ToCIDR(ipFrom, ipTo)

    @Throws(UnknownHostException::class)
    fun cidrToIpv4(cidr: String): Array<String> = ipTools.CIDRToIPv4(cidr)

    @Throws(UnknownHostException::class)
    fun cidrToIpv6(cidr: String): Array<String> = ipTools.CIDRToIPv6(cidr)
}
