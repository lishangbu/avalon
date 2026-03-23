package io.github.lishangbu.avalon.ip2location.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.ResourceUtils

/**
 * IP2Location 配置属性
 *
 * 绑定前缀：`ip2location`
 *
 * @author lishangbu
 * @since 2025/4/12
 */
@ConfigurationProperties(IpToLocationProperties.PREFIX)
class IpToLocationProperties {
    /** DB 文件位置 */
    var dbFileLocation: String =
        ResourceUtils.CLASSPATH_URL_PREFIX + "IP2LOCATION-LITE-DB11.IPV6.BIN"

    companion object {
        /** 配置前缀 */
        const val PREFIX: String = "ip2location"
    }
}
