package io.github.lishangbu.avalon.pokeapi.service.impl;

import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 抽象的PokeApi服务
 *
 * @author lishangbu
 * @since 2025/5/21
 */
public abstract class AbstractPokeApiService {
  protected static final Logger log = LoggerFactory.getLogger(AbstractPokeApiService.class);

  @Autowired
  @Qualifier("pokeApiRestClient")
  protected RestClient restClient;

  /**
   * 通过指定的URI和参数获取指定类型的数据实体
   *
   * @param responseType 响应数据的类型
   * @param uri 请求的URI模板
   * @param uriVariables URI中的参数
   * @return 指定类型的数据实体
   */
  protected <T> T getEntityFromUri(Class<T> responseType, String uri, Object... uriVariables) {
    log.info(
        "从URI [{}] 获取数据，参数: [{}]，响应类型: [{}]",
        uri,
        uriVariables.length > 0 ? uriVariables : "无",
        responseType.getSimpleName());
    try {
      return restClient
          .get()
          .uri(uri, uriVariables)
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
  protected NamedAPIResourceList listNamedAPIResources(String uri, Integer offset, Integer limit) {
    log.info("从URI [{}] 获取数据，偏移量：[{}]，数量：[{}]", uri, offset, limit);
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
