package io.github.lishangbu.avalon.ip2location.util;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.List;
import net.renfei.ip2location.IPTools;

/// IP 工具类
///
/// 提供 IP 地址格式识别与相互转换的工具方法，封装第三方 IPTools 功能
///
/// @author lishangbu
/// @since 2025/4/12
public class IpUtils {

  private static final class IpToolsHolder {
    static final IPTools IP_TOOLS = new IPTools();
  }

  /// This function checks if the string contains an IPv4 address.
  ///
  /// @param IPAddress IP Address to check
  /// @return Boolean
  public static boolean isIpv4(String IPAddress) {
    return IpToolsHolder.IP_TOOLS.IsIPv4(IPAddress);
  }

  /// This function checks if the string contains an IPv6 address.
  ///
  /// @param IPAddress IP Address to check
  /// @return Boolean
  public static boolean isIpv6(String IPAddress) {
    return IpToolsHolder.IP_TOOLS.IsIPv6(IPAddress);
  }

  /// This function converts IPv4 to IP number.
  ///
  /// @param IPAddress IP Address you wish to convert
  /// @return BigInteger
  public static BigInteger ipv4ToDecimal(String IPAddress) {
    return IpToolsHolder.IP_TOOLS.IPv4ToDecimal(IPAddress);
  }

  /// This function converts IPv6 to IP number.
  ///
  /// @param IPAddress IP Address you wish to convert
  /// @return BigInteger
  public static BigInteger ipv6ToDecimal(String IPAddress) {
    return IpToolsHolder.IP_TOOLS.IPv6ToDecimal(IPAddress);
  }

  /// This function converts IP number to IPv4.
  ///
  /// @param IPNum IP number you wish to convert
  /// @return String
  /// @throws UnknownHostException If unable to convert byte array to IP address
  public static String decimalToIpv4(BigInteger IPNum) throws UnknownHostException {
    return IpToolsHolder.IP_TOOLS.DecimalToIPv4(IPNum);
  }

  /// This function converts IP number to IPv6.
  ///
  /// @param IPNum IP number you wish to convert
  /// @return String
  /// @throws UnknownHostException If unable to convert byte array to IP address
  public static String decimalToIpv6(BigInteger IPNum) throws UnknownHostException {
    return IpToolsHolder.IP_TOOLS.DecimalToIPv6(IPNum);
  }

  /// This function returns the compressed form of the IPv6.
  ///
  /// @param IPAddress IP Address you wish to compress
  /// @return String
  public static String compressIpv6(String IPAddress) {
    return IpToolsHolder.IP_TOOLS.CompressIPv6(IPAddress);
  }

  /// This function returns the expanded form of the IPv6.
  ///
  /// @param IPAddress IP Address you wish to expand
  /// @return String
  public static String expandIpv6(String IPAddress) {
    return IpToolsHolder.IP_TOOLS.ExpandIPv6(IPAddress);
  }

  /// This function returns the CIDR for an IPv4 range.
  ///
  /// @param IPFrom Starting IP of the range
  /// @param IPTo Ending IP of the range
  /// @return List of strings
  /// @throws UnknownHostException If unable to convert byte array to IP address
  public static List<String> ipv4ToCidr(String IPFrom, String IPTo) throws UnknownHostException {
    return IpToolsHolder.IP_TOOLS.IPv4ToCIDR(IPFrom, IPTo);
  }

  /// This function returns the CIDR for an IPv6 range.
  ///
  /// @param IPFrom Starting IP of the range
  /// @param IPTo Ending IP of the range
  /// @return List of strings
  /// @throws UnknownHostException If unable to convert byte array to IP address
  public static List<String> ipv6ToCidr(String IPFrom, String IPTo) throws UnknownHostException {
    return IpToolsHolder.IP_TOOLS.IPv6ToCIDR(IPFrom, IPTo);
  }

  /// This function returns the IPv4 range for a CIDR.
  ///
  /// @param CIDR CIDR address to convert to range
  /// @return Array of strings
  /// @throws UnknownHostException If unable to convert byte array to IP address
  public static String[] cidrToIpv4(String CIDR) throws UnknownHostException {
    return IpToolsHolder.IP_TOOLS.CIDRToIPv4(CIDR);
  }

  /// This function returns the IPv6 range for a CIDR.
  ///
  /// @param CIDR CIDR address to convert to range
  /// @return Array of strings
  /// @throws UnknownHostException If unable to convert byte array to IP address
  public static String[] cidrToIpv6(String CIDR) throws UnknownHostException {
    return IpToolsHolder.IP_TOOLS.CIDRToIPv6(CIDR);
  }
}
