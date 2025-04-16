package io.github.lishangbu.avalon.security.util;

import io.github.lishangbu.avalon.security.properties.SecurityProperties;
import io.github.lishangbu.avalon.web.util.AbstractUrlIgnoreCache;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * 安全路径忽略缓存
 *
 * @author lishangbu
 * @since 2024/2/7
 */
@AutoConfiguration
public class SecurityUrlIgnoreCache extends AbstractUrlIgnoreCache implements InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(SecurityUrlIgnoreCache.class);
  private final SecurityProperties securityProperties;

  private List<String> ignoreUrls;

  public SecurityUrlIgnoreCache(SecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  @Override
  protected List<String> getIgnoreUrls() {
    return ignoreUrls;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    List<String> ignoreUrls = securityProperties.getIgnoreUrls();
    if (log.isInfoEnabled()) {
      log.info("忽略URL路径安全校验:[{}]", String.join(",", ignoreUrls));
    }
    this.ignoreUrls = ignoreUrls;
  }
}
