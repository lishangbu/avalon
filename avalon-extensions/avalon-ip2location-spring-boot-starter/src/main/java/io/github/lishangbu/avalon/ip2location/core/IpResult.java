package io.github.lishangbu.avalon.ip2location.core;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.renfei.ip2location.IPResult;

/**
 * 该类表示IP查询的结果，包括IP相关的详细信息。 它封装了从IP数据库中获取到的各种信息，如地址类型、地理位置、网络信息等。
 *
 * @author lishangbu
 * @since 2025/4/12
 */
@ToString
@Setter
@Getter
@NoArgsConstructor
public class IpResult {

  /** 地址类型，例如 IPv4 或 IPv6 */
  private String addressType;

  /** 区号 */
  private String areaCode;

  /** 自动系统（AS） */
  private String as;

  /** ASN编号 */
  private String asn;

  /** 类别 */
  private String category;

  /** 城市 */
  private String city;

  /** 国家简称 */
  private String countryShort;

  /** 国家全称 */
  private String countryLong;

  /** 是否延迟 */
  private boolean delay;

  /** 区域 */
  private String district;

  /** 域名 */
  private String domain;

  /** 海拔 */
  private float elevation;

  /** 国际直接拨号代码 */
  private String iddCode;

  /** 互联网服务提供商 */
  private String isp;

  /** 纬度 */
  private float latitude;

  /** 经度 */
  private float longitude;

  /** 移动品牌 */
  private String mobileBrand;

  /** 移动国家代码 */
  private String mcc;

  /** 移动网络代码 */
  private String mnc;

  /** 网络速度 */
  private String netSpeed;

  /** 区域 */
  private String region;

  /** 状态 */
  private String status;

  /** 时区 */
  private String timezone;

  /** 使用类型 */
  private String usageType;

  /** 版本 */
  private String version;

  /** 气象站代码 */
  private String weatherStationCode;

  /** 气象站名称 */
  private String weatherStationName;

  /** 邮政编码 */
  private String zipcode;

  /**
   * 使用原始的IP查询结果构造一个新的IpResult对象。
   *
   * @param originResult 原始的IP查询结果
   */
  public IpResult(IPResult originResult) {
    this.addressType = originResult.getAddressType();
    this.as = originResult.getAS();
    this.asn = originResult.getASN();
    this.version = originResult.getVersion();
    this.delay = originResult.getDelay();
    this.status = originResult.getStatus();
    this.district = originResult.getDistrict();
    this.category = originResult.getCategory();
    this.usageType = originResult.getUsageType();
    this.elevation = originResult.getElevation();
    this.mobileBrand = originResult.getMobileBrand();
    this.mnc = originResult.getMNC();
    this.mcc = originResult.getMCC();
    this.weatherStationName = originResult.getWeatherStationName();
    this.weatherStationCode = originResult.getWeatherStationCode();
    this.areaCode = originResult.getAreaCode();
    this.iddCode = originResult.getIDDCode();
    this.timezone = originResult.getTimeZone();
    this.netSpeed = originResult.getNetSpeed();
    this.zipcode = originResult.getZipCode();
    this.domain = originResult.getDomain();
    this.longitude = originResult.getLongitude();
    this.latitude = originResult.getLatitude();
    this.isp = originResult.getISP();
    this.city = originResult.getCity();
    this.countryShort = originResult.getCountryShort();
    this.countryLong = originResult.getCountryLong();
    this.region = originResult.getRegion();
  }
}
