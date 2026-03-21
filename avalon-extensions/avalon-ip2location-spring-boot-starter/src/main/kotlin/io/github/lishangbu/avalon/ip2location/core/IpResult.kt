package io.github.lishangbu.avalon.ip2location.core

import net.renfei.ip2location.IPResult

/**
 * IP 查询结果封装 表示从 IP 数据库获取到的详细信息（地址类型、地理位置、网络信息等）
 *
 * @param addressType 地址类型
 * @param areaCode 区域代码
 * @param as AS 信息
 * @param asn ASN 信息
 * @param category 类别信息
 * @param city 城市
 * @param countryShort 国家缩写
 * @param countryLong 国家全称
 * @param delay 延迟信息
 * @param district 区域
 * @param domain 域名
 * @param elevation 海拔高度
 * @param iddCode 国际拨号代码
 * @param isp 网络服务提供商
 * @param latitude 纬度
 * @param longitude 经度
 * @param mobileBrand 移动品牌
 * @param mcc 移动国家代码
 * @param mnc 移动网络代码
 * @param netSpeed 网络速度
 * @param region 所属地区
 * @param status 状态
 * @param timezone 时区
 * @param usageType 使用类型
 * @param version 版本
 * @param weatherStationCode 气象站代码
 * @param weatherStationName 气象站名称
 * @param zipcode 邮政编码 使用原始的IP查询结果构造一个新的IpResult对象
 * @param originResult 原始的IP查询结果
 */

/**
 * IP 查询结果封装。
 *
 * 表示从 IP 数据库获取到的详细信息（地址类型、地理位置、网络信息等）。
 *
 * @author lishangbu
 * @since 2025/4/12
 */
data class IpResult(
    val addressType: String,
    val areaCode: String,
    val `as`: String,
    val asn: String,
    val category: String,
    val city: String,
    val countryShort: String,
    val countryLong: String,
    val delay: Boolean,
    val district: String,
    val domain: String,
    val elevation: Float,
    val iddCode: String,
    val isp: String,
    val latitude: Float,
    val longitude: Float,
    val mobileBrand: String,
    val mcc: String,
    val mnc: String,
    val netSpeed: String,
    val region: String,
    val status: String,
    val timezone: String,
    val usageType: String,
    val version: String,
    val weatherStationCode: String,
    val weatherStationName: String,
    val zipcode: String,
) {
    /**
     * 使用原始的 IP 查询结果构造一个新的 [IpResult] 对象。
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
