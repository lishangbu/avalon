package io.github.lishangbu.avalon.pokeapi.properties;

import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PokeAPI服务配置属性
 *
 * <p>包含连接PokeAPI服务的基本配置以及本地缓存设置。 默认使用官方API并启用本地文件缓存来减少API调用次数。
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@ConfigurationProperties(prefix = PokeApiProperties.POKE_API_PROPERTIES_PREFIX)
public class PokeApiProperties {
  /** 配置属性前缀 */
  public static final String POKE_API_PROPERTIES_PREFIX = "pokeapi";

  /** POKE API DATA {@link PokeApiProperties#getRemoteRepoUrl()}git仓库</a>的本地存储路径 */
  private String localRepoDir =
      Paths.get(System.getProperty("user.home"), ".avalon", "api-data").toString();

  /** POKE API DATA git仓库地址,默认为<a href="https://github.com/PokeAPI/api-data.git">官方git仓库</a> */
  private String remoteRepoUrl = "https://github.com/PokeAPI/api-data.git";

  /**
   * 获取PokeAPIData的git仓库本地存储路径
   *
   * @return PokeAPIData的git仓库本地存储路径
   */
  public String getLocalRepoDir() {
    return localRepoDir;
  }

  /**
   * 设置PokeAPIData的git仓库本地存储路径
   *
   * @param localRepoDir PokeAPIData的git仓库本地存储路径
   */
  public void setLocalRepoDir(String localRepoDir) {
    this.localRepoDir = localRepoDir;
  }

  public String getRemoteRepoUrl() {
    return remoteRepoUrl;
  }

  public void setRemoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
  }
}
