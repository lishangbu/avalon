package io.github.lishangbu.avalon.ip2location.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.ResourceUtils;

/// IP2Location 配置属性
///
/// 绑定前缀：`ip2location`
///
/// @author lishangbu
/// @since 2025/4/12
@ConfigurationProperties(IpToLocationProperties.PREFIX)
public class IpToLocationProperties {
    public static final String PREFIX = "ip2location";

    /// ip2region.db 文件路径，默认加载同时包含 IPv4 和 IPv6 的数据库文件
    private String dbFileLocation =
            ResourceUtils.CLASSPATH_URL_PREFIX + "IP2LOCATION-LITE-DB11.IPV6.BIN";

    public String getDbFileLocation() {
        return dbFileLocation;
    }

    public void setDbFileLocation(String dbFileLocation) {
        this.dbFileLocation = dbFileLocation;
    }
}
