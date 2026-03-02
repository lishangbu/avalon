package io.github.lishangbu.avalon.ip2location.util;

import lombok.experimental.UtilityClass;
import net.renfei.ip2location.IPTools;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.List;

/// IP 工具类
///
/// 提供 IP 地址格式识别与相互转换的工具方法，封装第三方 IPTools 功能
///
/// @author lishangbu
/// @since 2025/4/12
@UtilityClass
public class IpUtils {

    /// 判断给定字符串是否为 IPv4 地址
    ///
    /// @param ipAddress 要检查的 IP 地址
    /// @return 如果是 IPv4 则返回 `true`，否则返回 `false`
    public boolean isIpv4(String ipAddress) {
        return IpToolsHolder.IP_TOOLS.IsIPv4(ipAddress);
    }

    /// 判断给定字符串是否为 IPv6 地址
    ///
    /// @param ipAddress 要检查的 IP 地址
    /// @return 如果是 IPv6 则返回 `true`，否则返回 `false`
    public boolean isIpv6(String ipAddress) {
        return IpToolsHolder.IP_TOOLS.IsIPv6(ipAddress);
    }

    /// 将 IPv4 地址转换为十进制表示
    ///
    /// @param ipAddress 要转换的 IPv4 地址
    /// @return 转换后的十进制 [BigInteger]
    public BigInteger ipv4ToDecimal(String ipAddress) {
        return IpToolsHolder.IP_TOOLS.IPv4ToDecimal(ipAddress);
    }

    /// 将 IPv6 地址转换为十进制表示
    ///
    /// @param ipAddress 要转换的 IPv6 地址
    /// @return 转换后的十进制  [BigInteger]
    public BigInteger ipv6ToDecimal(String ipAddress) {
        return IpToolsHolder.IP_TOOLS.IPv6ToDecimal(ipAddress);
    }

    /// 将十进制表示转换为 IPv4 地址字符串
    ///
    /// @param ipNumber 要转换的十进制 IP 值
    /// @return 转换后的 IPv4 地址字符串
    /// @throws UnknownHostException 当无法将字节数组转换为 IP 地址时抛出
    public String decimalToIpv4(BigInteger ipNumber) throws UnknownHostException {
        return IpToolsHolder.IP_TOOLS.DecimalToIPv4(ipNumber);
    }

    /// 将十进制表示转换为 IPv6 地址字符串
    ///
    /// @param ipNumber 要转换的十进制 IP 值
    /// @return 转换后的 IPv6 地址字符串
    /// @throws UnknownHostException 当无法将字节数组转换为 IP 地址时抛出
    public String decimalToIpv6(BigInteger ipNumber) throws UnknownHostException {
        return IpToolsHolder.IP_TOOLS.DecimalToIPv6(ipNumber);
    }

    /// 将 IPv6 地址压缩为简短形式
    ///
    /// @param ipAddress 要压缩的 IPv6 地址
    /// @return 压缩后的 IPv6 字符串
    public String compressIpv6(String ipAddress) {
        return IpToolsHolder.IP_TOOLS.CompressIPv6(ipAddress);
    }

    /// 将 IPv6 地址展开为完整形式
    ///
    /// @param ipAddress 要展开的 IPv6 地址
    /// @return 展开后的 IPv6 字符串
    public String expandIpv6(String ipAddress) {
        return IpToolsHolder.IP_TOOLS.ExpandIPv6(ipAddress);
    }

    /// 将 IPv4 范围转换为 CIDR 表示法列表
    ///
    /// @param ipFrom 范围起始 IPv4 地址
    /// @param ipTo   范围结束 IPv4 地址
    /// @return CIDR 字符串列表
    /// @throws UnknownHostException 当无法将字节数组转换为 IP 地址时抛出
    public List<String> ipv4ToCidr(String ipFrom, String ipTo) throws UnknownHostException {
        return IpToolsHolder.IP_TOOLS.IPv4ToCIDR(ipFrom, ipTo);
    }

    /// 将 IPv6 范围转换为 CIDR 表示法列表
    ///
    /// @param ipFrom 范围起始 IPv6 地址
    /// @param ipTo   范围结束 IPv6 地址
    /// @return CIDR 字符串列表
    /// @throws UnknownHostException 当无法将字节数组转换为 IP 地址时抛出
    public List<String> ipv6ToCidr(String ipFrom, String ipTo) throws UnknownHostException {
        return IpToolsHolder.IP_TOOLS.IPv6ToCIDR(ipFrom, ipTo);
    }

    /// 将 CIDR 表示的 IPv4 转换为起止地址数组
    ///
    /// @param CIDR 要转换的 CIDR 表示法
    /// @return 包含起始和结束 IPv4 的字符串数组
    /// @throws UnknownHostException 当无法将字节数组转换为 IP 地址时抛出
    public String[] cidrToIpv4(String CIDR) throws UnknownHostException {
        return IpToolsHolder.IP_TOOLS.CIDRToIPv4(CIDR);
    }

    /// 将 CIDR 表示的 IPv6 转换为起止地址数组
    ///
    /// @param CIDR 要转换的 CIDR 表示法
    /// @return 包含起始和结束 IPv6 的字符串数组
    /// @throws UnknownHostException 当无法将字节数组转换为 IP 地址时抛出
    public String[] cidrToIpv6(String CIDR) throws UnknownHostException {
        return IpToolsHolder.IP_TOOLS.CIDRToIPv6(CIDR);
    }

    private static final class IpToolsHolder {
        static final IPTools IP_TOOLS = new IPTools();
    }
}
