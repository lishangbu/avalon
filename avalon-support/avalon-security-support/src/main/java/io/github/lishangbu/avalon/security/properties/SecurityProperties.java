package io.github.lishangbu.avalon.security.properties;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全配置项
 *
 * @author lishangbu
 * @since 2023/10/6
 */
@ConfigurationProperties(prefix = SecurityProperties.SECURITY_PROPERTIES_PREFIX)
public class SecurityProperties {

  public static final String SECURITY_PROPERTIES_PREFIX = "spring.security";

  private List<String> ignoreUrls = new ArrayList<>();

  public List<String> getIgnoreUrls() {
    return ignoreUrls;
  }

  public void setIgnoreUrls(List<String> ignoreUrls) {
    this.ignoreUrls = ignoreUrls;
  }
}
