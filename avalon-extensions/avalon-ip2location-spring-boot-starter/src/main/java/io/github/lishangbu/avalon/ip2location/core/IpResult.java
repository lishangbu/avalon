package io.github.lishangbu.avalon.ip2location.core;

import java.beans.ConstructorProperties;
import net.renfei.ip2location.IPResult;

/// IP 查询结果封装
///
/// 表示从 IP 数据库获取到的详细信息（地址类型、地理位置、网络信息等）
///
/// @param addressType        地址类型
/// @param areaCode           区域代码
/// @param as                 AS 信息
/// @param asn                ASN 信息
/// @param category           类别信息
/// @param city               城市
/// @param countryShort       国家缩写
/// @param countryLong        国家全称
/// @param delay              延迟信息
/// @param district           区域
/// @param domain             域名
/// @param elevation          海拔高度
/// @param iddCode            国际拨号代码
/// @param isp                网络服务提供商
/// @param latitude           纬度
/// @param longitude          经度
/// @param mobileBrand        移动品牌
/// @param mcc                移动国家代码
/// @param mnc                移动网络代码
/// @param netSpeed           网络速度
/// @param region             所属地区
/// @param status             状态
/// @param timezone           时区
/// @param usageType          使用类型
/// @param version            版本
/// @param weatherStationCode 气象站代码
/// @param weatherStationName 气象站名称
/// @param zipcode            邮政编码
/// @author lishangbu
/// @since 2025/4/12
public record IpResult(
    String addressType,
    String areaCode,
    String as,
    String asn,
    String category,
    String city,
    String countryShort,
    String countryLong,
    boolean delay,
    String district,
    String domain,
    float elevation,
    String iddCode,
    String isp,
    float latitude,
    float longitude,
    String mobileBrand,
    String mcc,
    String mnc,
    String netSpeed,
    String region,
    String status,
    String timezone,
    String usageType,
    String version,
    String weatherStationCode,
    String weatherStationName,
    String zipcode) {

  /// 使用原始的IP查询结果构造一个新的IpResult对象
  ///
  /// @param originResult 原始的IP查询结果
  @ConstructorProperties({
    "addressType", "areaCode", "as", "asn", "category", "city", "countryShort", "countryLong",
    "delay", "district", "domain", "elevation", "iddCode", "isp", "latitude", "longitude",
    "mobileBrand", "mcc", "mnc", "netSpeed", "region", "status", "timezone", "usageType",
    "version", "weatherStationCode", "weatherStationName", "zipcode"
  })
  public IpResult(IPResult originResult) {
    this(
        originResult.getAddressType(),
        originResult.getAreaCode(),
        originResult.getAS(),
        originResult.getASN(),
        originResult.getCategory(),
        originResult.getCity(),
        originResult.getCountryShort(),
        originResult.getCountryLong(),
        originResult.getDelay(),
        originResult.getDistrict(),
        originResult.getDomain(),
        originResult.getElevation(),
        originResult.getIDDCode(),
        originResult.getISP(),
        originResult.getLatitude(),
        originResult.getLongitude(),
        originResult.getMobileBrand(),
        originResult.getMCC(),
        originResult.getMNC(),
        originResult.getNetSpeed(),
        originResult.getRegion(),
        originResult.getStatus(),
        originResult.getTimeZone(),
        originResult.getUsageType(),
        originResult.getVersion(),
        originResult.getWeatherStationCode(),
        originResult.getWeatherStationName(),
        originResult.getZipCode());
  }
}
