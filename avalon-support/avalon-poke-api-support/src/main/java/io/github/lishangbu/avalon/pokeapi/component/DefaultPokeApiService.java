package io.github.lishangbu.avalon.pokeapi.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.properties.PokeApiProperties;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 抽象的PokeApi服务
 *
 * @author lishangbu
 * @since 2025/5/21
 */
public class DefaultPokeApiService implements PokeApiService {
  private static final Logger log = LoggerFactory.getLogger(DefaultPokeApiService.class);

  private static final ObjectMapper FILE_CACHE_OBJECT_MAPPER = new ObjectMapper();

  private final RestClient restClient;

  private final PokeApiProperties properties;

  public DefaultPokeApiService(RestClient restClient, PokeApiProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  /**
   * 通过指定的URI和参数获取指定类型的数据实体
   *
   * @param responseType 响应数据的类型
   * @param uri 请求的URI模板
   * @param idOrName URI中的参数,可以是ID，也可以是name
   * @return 指定类型的数据实体
   */
  @Override
  public <T> T getEntityFromUri(Class<T> responseType, String uri, Serializable idOrName) {
    log.debug("从URI [{}] 获取数据，参数: [{}]，响应类型: [{}]", uri, idOrName, responseType.getSimpleName());
    String fileCacheKey = responseType.getSimpleName() + ":" + uri + ":" + idOrName;

    // 尝试从文件缓存获取数据
    T cachedResult = getFromFileCache(fileCacheKey, responseType);
    if (cachedResult != null) {
      return cachedResult;
    }

    try {
      String fullUri = uri + "/" + idOrName;
      log.debug("发起API请求: {}", fullUri);
      T body =
          restClient
              .get()
              .uri(fullUri)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .toEntity(responseType)
              .getBody();

      // 保存结果到文件缓存
      saveToFileCache(fileCacheKey, body);

      return body;
    } catch (Exception e) {
      log.error("请求API失败，URI：[{}]，错误信息：[{}]", uri, e.getMessage());
      throw new RuntimeException("API请求失败", e);
    }
  }

  /**
   * 获取带有分页信息的命名资源列表
   *
   * @param uri 请求的URI
   * @param offset 偏移量
   * @param limit 返回数量限制
   * @return 命名资源列表
   */
  @Override
  public NamedAPIResourceList listNamedAPIResources(String uri, Integer offset, Integer limit) {
    log.debug("从URI [{}] 获取数据，偏移量：[{}]，数量：[{}]", uri, offset, limit);
    String finalUri =
        UriComponentsBuilder.fromUriString(uri)
            .queryParam("offset", offset)
            .queryParam("limit", limit)
            .toUriString();

    String fileCacheKey = uri + ":" + offset + ":" + limit;

    // 尝试从文件缓存获取数据
    NamedAPIResourceList cachedResult = getFromFileCache(fileCacheKey, NamedAPIResourceList.class);
    if (cachedResult != null) {
      return cachedResult;
    }

    try {
      log.debug("发起API请求: {}", finalUri);
      NamedAPIResourceList body =
          restClient
              .get()
              .uri(finalUri)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .toEntity(NamedAPIResourceList.class)
              .getBody();

      // 保存结果到文件缓存
      saveToFileCache(fileCacheKey, body);

      return body;
    } catch (Exception e) {
      log.error("请求API失败，URI：[{}]，错误信息：[{}]", finalUri, e.getMessage());
      throw new RuntimeException("API请求失败", e);
    }
  }

  /**
   * 从文件缓存中获取数据
   *
   * @param cacheKey 缓存键
   * @param responseType 响应类型
   * @return 缓存的数据，如果不存在或出错则返回null
   */
  private <T> T getFromFileCache(String cacheKey, Class<T> responseType) {
    if (!properties.getEnableFileCache()) {
      return null;
    }

    try {
      Path filePath = Paths.get(properties.getFileCachePath(), cacheKey);
      File file = filePath.toFile();
      if (file.exists()) {
        log.debug("从文件缓存获取数据: {}", filePath);
        return FILE_CACHE_OBJECT_MAPPER.readValue(Files.readString(filePath), responseType);
      }
    } catch (FileNotFoundException e) {
      log.info("文件缓存未找到: {}", cacheKey);
    } catch (IOException e) {
      log.warn("文件缓存读取失败: {}, 原因: {}", cacheKey, e.getMessage());
    }
    return null;
  }

  /**
   * 保存数据到文件缓存
   *
   * @param cacheKey 缓存键
   * @param data 要缓存的数据
   */
  private void saveToFileCache(String cacheKey, Object data) {
    if (!properties.getEnableFileCache() || data == null) {
      return;
    }

    try {
      Path dirPath = Paths.get(properties.getFileCachePath());
      // 确保缓存目录存在
      if (!dirPath.toFile().exists()) {
        Files.createDirectories(dirPath);
      }

      Path filePath = dirPath.resolve(cacheKey);
      log.debug("保存数据到文件缓存: {}", filePath);
      Files.writeString(filePath, FILE_CACHE_OBJECT_MAPPER.writeValueAsString(data));
    } catch (IOException e) {
      log.warn("保存到文件缓存失败: {}, 原因: {}", cacheKey, e.getMessage());
    }
  }
}
