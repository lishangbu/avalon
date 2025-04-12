package io.github.lishangbu.avalon.ip2location.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.ResourceUtils;

/**
 * IP2location配置
 *
 * @author lishangbu
 * @since 2025/4/12
 */
@Data
@ConfigurationProperties(IpToLocationProperties.PREFIX)
public class IpToLocationProperties {
  public static final String PREFIX = "ip2location";

  /** ip2region.db 文件路径 默认加载包含ipv4和ipv6的数据 */
  private String dbFileLocation =
      ResourceUtils.CLASSPATH_URL_PREFIX + "IP2LOCATION-LITE-DB11.IPV6.BIN";
}
