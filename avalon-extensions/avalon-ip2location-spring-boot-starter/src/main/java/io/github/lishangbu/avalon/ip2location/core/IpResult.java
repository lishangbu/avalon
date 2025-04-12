package io.github.lishangbu.avalon.ip2location.core;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.renfei.ip2location.IPResult;

/**
 * Ip结果
 *
 * @author lishangbu
 * @since 2025/4/12
 */
@ToString
@Setter
@Getter
@NoArgsConstructor
public class IpResult {
  private String addressType;
  private String areaCode;

  private String as;

  private String asn;
  private String category;

  private String city;

  private String countryShort;
  private String countryLong;
  private boolean delay;
  private String district;

  private String domain;
  private float elevation;

  private String iddCode;
  private String isp;
  private float latitude;
  private float longitude;
  private String mobileBrand;

  private String mcc;
  private String mnc;

  private String netSpeed;
  private String region;
  private String status;
  private String timezone;
  private String usageType;

  private String version;
  private String weatherStationCode;
  private String weatherStationName;
  private String zipcode;

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
