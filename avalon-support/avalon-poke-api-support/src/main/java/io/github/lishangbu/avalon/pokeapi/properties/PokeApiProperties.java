package io.github.lishangbu.avalon.pokeapi.properties;

import java.nio.file.Path;
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

  /** PokeAPI基础URL，默认使用官方API地址 可通过配置文件修改，例如使用自托管的API服务 */
  private String apiUrl = "https://pokeapi.co/api/v2/";

  /** 是否启用文件缓存 默认启用，以减少API请求次数并提高响应速度 */
  private Boolean enableFileCache = true;

  /** 文件缓存路径 默认使用系统临时目录下的pokeapi-cache文件夹 */
  private String fileCachePath =
      Paths.get(System.getProperty("java.io.tmpdir"), "pokeapi-cache").toString();

  /**
   * 获取PokeAPI的基础URL
   *
   * @return API基础URL
   */
  public String getApiUrl() {
    return apiUrl;
  }

  /**
   * 设置PokeAPI的基础URL
   *
   * @param apiUrl API基础URL
   */
  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  /**
   * 检查是否启用了文件缓存
   *
   * @return 如果启用文件缓存则返回true，否则返回false
   */
  public Boolean getEnableFileCache() {
    return enableFileCache;
  }

  /**
   * 设置是否启用文件缓存
   *
   * @param enableFileCache 启用文件缓存的标志
   */
  public void setEnableFileCache(Boolean enableFileCache) {
    this.enableFileCache = enableFileCache;
  }

  /**
   * 获取文件缓存的路径
   *
   * @return 文件缓存路径
   */
  public String getFileCachePath() {
    return fileCachePath;
  }

  /**
   * 设置��件缓存的路径
   *
   * @param fileCachePath 文件缓存路径
   */
  public void setFileCachePath(String fileCachePath) {
    this.fileCachePath = fileCachePath;
  }

  /**
   * 获取文件缓存的路径作为Path对象
   *
   * @return 文件缓存路径的Path对象
   */
  public Path getFileCachePathAsPath() {
    return Paths.get(fileCachePath);
  }
}
