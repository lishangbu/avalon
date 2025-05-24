package io.github.lishangbu.avalon.pokeapi.component;

import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import java.io.Serializable;
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

  private RestClient restClient;

  public DefaultPokeApiService(RestClient restClient) {
    this.restClient = restClient;
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
    try {
      return restClient
          .get()
          .uri(uri + "/" + idOrName)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .toEntity(responseType)
          .getBody();
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
    try {
      return restClient
          .get()
          .uri(finalUri)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .toEntity(NamedAPIResourceList.class)
          .getBody();
    } catch (Exception e) {
      log.error("请求API失败，URI：[{}]，错误信息：[{}]", finalUri, e.getMessage());
      throw new RuntimeException("API请求失败", e);
    }
  }
}
