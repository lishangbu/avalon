package io.github.lishangbu.avalon.ip2location.core;

import java.beans.ConstructorProperties;
import net.renfei.ip2location.IPResult;

/**
 * 该类表示IP查询的结果，包括IP相关的详细信息。 它封装了从IP数据库中获取到的各种信息，如地址类型、地理位置、网络信息等。
 *
 * @author lishangbu
 * @since 2025/4/12
 */
public record IpResult(
    /** 地址类型 */
    String addressType,

    /** 区域代码 */
    String areaCode,

    /** AS 信息 */
    String as,

    /** ASN 信息 */
    String asn,

    /** 类别信息 */
    String category,

    /** 城市 */
    String city,

    /** 国家缩写 */
    String countryShort,

    /** 国家全称 */
    String countryLong,

    /** 延迟信息 */
    boolean delay,

    /** 区域 */
    String district,

    /** 域名 */
    String domain,

    /** 海拔高度 */
    float elevation,

    /** 国际拨号代码 */
    String iddCode,

    /** 网络服务提供商 */
    String isp,

    /** 纬度 */
    float latitude,

    /** 经度 */
    float longitude,

    /** 移动品牌 */
    String mobileBrand,

    /** 移动国家代码 */
    String mcc,

    /** 移动网络代码 */
    String mnc,

    /** 网络速度 */
    String netSpeed,

    /** 所属地区 */
    String region,

    /** 状态 */
    String status,

    /** 时区 */
    String timezone,

    /** 使用类型 */
    String usageType,

    /** 版本 */
    String version,

    /** 气象站代码 */
    String weatherStationCode,

    /** 气象站名称 */
    String weatherStationName,

    /** 邮政编码 */
    String zipcode) {

  /**
   * 使用原始的IP查询结果构造一个新的IpResult对象。
   *
   * @param originResult 原始的IP查询结果
   */
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
