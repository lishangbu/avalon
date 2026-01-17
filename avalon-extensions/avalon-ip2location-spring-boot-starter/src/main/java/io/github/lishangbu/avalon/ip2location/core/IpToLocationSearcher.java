package io.github.lishangbu.avalon.ip2location.core;

import io.github.lishangbu.avalon.ip2location.exception.*;
import io.github.lishangbu.avalon.ip2location.properties.IpToLocationProperties;
import java.io.IOException;
import java.io.InputStream;
import net.renfei.ip2location.IP2Location;
import net.renfei.ip2location.IPResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

/// IP 搜索器
///
/// 封装 IP2Location 的查询与资源加载逻辑，支持初始化与销毁钩子
///
/// @author lishangbu
/// @since 2025/4/12
public class IpToLocationSearcher implements InitializingBean, DisposableBean {

  private static final Logger log = LoggerFactory.getLogger(IpToLocationSearcher.class);
  private final IpToLocationProperties ipToLocationProperties;

  private final ResourceLoader resourceLoader;

  private IP2Location loc;

  public IpToLocationSearcher(
      IpToLocationProperties ipToLocationProperties, ResourceLoader resourceLoader) {
    this.ipToLocationProperties = ipToLocationProperties;
    this.resourceLoader = resourceLoader;
  }

  /// This function to query IP2Location data.
  ///
  /// @param ipAddress IP Address you wish to query
  /// @return IP2Location data
  /// @throws IOException If an input or output exception occurred
  public IpResult ipQuery(String ipAddress) {
    if (loc == null) {
      return null;
    }
    try {
      IPResult rec = loc.IPQuery(ipAddress);
      if ("OK".equals(rec.getStatus())) {
        return new IpResult(rec);
      } else if ("EMPTY_IP_ADDRESS".equals(rec.getStatus())) {
        log.error("IP address cannot be blank.");
        throw new EmptyIpAddressException();
      } else if ("INVALID_IP_ADDRESS".equals(rec.getStatus())) {
        log.error("Invalid IP address.IpAddress:[{}] is invalid", ipAddress);
        throw new InvalidIpAddressException();
      } else if ("MISSING_FILE".equals(rec.getStatus())) {
        log.error(
            "Invalid database path.current database path is :[{}]",
            ipToLocationProperties.getDbFileLocation());
        throw new MissingFileException();
      } else if ("IPV6_NOT_SUPPORTED".equals(rec.getStatus())) {
        log.error("This BIN does not contain IPv6 data.IpAddress:[{}]is a v6 format ip", ipAddress);
        throw new Ipv6NotSupportException();
      } else {
        log.error("Unknown error.[{}]", rec.getStatus());
        throw new IpToLocationException("Unknown error." + rec.getStatus());
      }
    } catch (IOException e) {
      log.error(
          "reading  database path failed.current database path is :[{}]",
          ipToLocationProperties.getDbFileLocation());
      throw new MissingFileException();
    }
  }

  @Override
  public void destroy() throws Exception {
    if (loc != null) {
      loc.Close();
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (loc == null) {
      loc = new IP2Location();
      Resource resource = resourceLoader.getResource(ipToLocationProperties.getDbFileLocation());
      try (InputStream inputStream = resource.getInputStream()) {
        loc.Open(StreamUtils.copyToByteArray(inputStream));
      }
    }
  }
}
