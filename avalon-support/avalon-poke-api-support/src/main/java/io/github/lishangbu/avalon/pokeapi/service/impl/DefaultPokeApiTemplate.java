package io.github.lishangbu.avalon.pokeapi.service.impl;

import io.github.lishangbu.avalon.pokeapi.model.pagination.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.type.Type;
import io.github.lishangbu.avalon.pokeapi.service.PokeApiTemplate;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 默认PokeApi请求模板
 *
 * @author lishangbu
 * @since 2025/5/21
 */
@Service
public class DefaultPokeApiTemplate implements PokeApiTemplate {
  private static final Logger log = LoggerFactory.getLogger(DefaultPokeApiTemplate.class);

  private final RestClient pokeApiRestClient;

  public DefaultPokeApiTemplate(RestClient pokeApiRestClient) {
    this.pokeApiRestClient = pokeApiRestClient;
  }

  @Override
  @Cacheable(value = "types", key = "#offset-#limit")
  public NamedAPIResourceList listTypes(Integer offset, Integer limit) {
    return listNamedAPIResources("/type", offset, limit);
  }

  @Override
  @Cacheable(value = "type", key = "#arg")
  public Type getType(Serializable arg) {
    return getEntityFromUri(Type.class, "/type/{arg}", arg);
  }

  // 提取的通用方法
  private <T> T getEntityFromUri(Class<T> responseType, String uri, Object... uriVariables) {
    log.info(
        "从[{}]获取数据，参数:[{}]，响应类型：[{}]",
        uri,
        uriVariables.length > 0 ? uriVariables : "无",
        responseType.getSimpleName());
    return this.pokeApiRestClient
        .get()
        .uri(uri, uriVariables) // 使用uri模板替代字符串拼接
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toEntity(responseType)
        .getBody();
  }

  private NamedAPIResourceList listNamedAPIResources(String uri, Integer offset, Integer limit) {
    log.info("从[{}]获取数据，偏移量：[{}]，数量：[{}]", uri, offset, limit);
    return this.pokeApiRestClient
        .get()
        .uri(uri + (uri.contains("?") ? "&" : "?") + "offset=" + offset + "&limit=" + limit)
        .retrieve()
        .toEntity(NamedAPIResourceList.class)
        .getBody();
  }
}
