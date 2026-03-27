package io.github.lishangbu.avalon.ip2location.core

import net.renfei.ip2location.IPResult

/**
 * IP 查询结果
 *
 * 表示从 IP 数据库返回的地址与网络信息
 *
 * @author lishangbu
 * @since 2025/4/12
 */
data class IpResult(
    /** 地址属性 */
    val addressType: String,
    /** 地区状态码 */
    val areaCode: String,
    /** AS 信息 */
    val `as`: String,
    /** ASN */
    val asn: String,
    /** 分类 */
    val category: String,
    /** 城市 */
    val city: String,
    /** 国家简称 */
    val countryShort: String,
    /** 国家全称 */
    val countryLong: String,
    /** 延迟 */
    val delay: Boolean,
    /** 区县 */
    val district: String,
    /** 域名 */
    val domain: String,
    /** 海拔 */
    val elevation: Float,
    /** 国际拨号代码 */
    val iddCode: String,
    /** ISP */
    val isp: String,
    /** 纬度 */
    val latitude: Float,
    /** 经度 */
    val longitude: Float,
    /** 移动品牌 */
    val mobileBrand: String,
    /** MCC */
    val mcc: String,
    /** MNC */
    val mnc: String,
    /** 网络速度 */
    val netSpeed: String,
    /** 地区 */
    val region: String,
    /** 状态 */
    val status: String,
    /** 时区 */
    val timezone: String,
    /** 用途属性 */
    val usageType: String,
    /** 版本 */
    val version: String,
    /** 气象站代码 */
    val weatherStationCode: String,
    /** 天气站点名称 */
    val weatherStationName: String,
    /** 邮编 */
    val zipcode: String,
) {
    /**
     * 使用原始查询结果构建实例
     *
     * @param originResult 原始的 IP 查询结果
     */
    constructor(
        originResult: IPResult,
    ) : this(
        addressType = originResult.getAddressType(),
        areaCode = originResult.getAreaCode(),
        `as` = originResult.getAS(),
        asn = originResult.getASN(),
        category = originResult.getCategory(),
        city = originResult.getCity(),
        countryShort = originResult.getCountryShort(),
        countryLong = originResult.getCountryLong(),
        delay = originResult.getDelay(),
        district = originResult.getDistrict(),
        domain = originResult.getDomain(),
        elevation = originResult.getElevation(),
        iddCode = originResult.getIDDCode(),
        isp = originResult.getISP(),
        latitude = originResult.getLatitude(),
        longitude = originResult.getLongitude(),
        mobileBrand = originResult.getMobileBrand(),
        mcc = originResult.getMCC(),
        mnc = originResult.getMNC(),
        netSpeed = originResult.getNetSpeed(),
        region = originResult.getRegion(),
        status = originResult.getStatus(),
        timezone = originResult.getTimeZone(),
        usageType = originResult.getUsageType(),
        version = originResult.getVersion(),
        weatherStationCode = originResult.getWeatherStationCode(),
        weatherStationName = originResult.getWeatherStationName(),
        zipcode = originResult.getZipCode(),
    )
}
