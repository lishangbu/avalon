package io.github.lishangbu.avalon.shell.pokeapi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.lishangbu.avalon.json.util.JsonUtils;
import io.github.lishangbu.avalon.shell.pokeapi.constant.PokeApiConstants;
import io.github.lishangbu.avalon.shell.pokeapi.model.PokeApiPagination;
import io.github.lishangbu.avalon.shell.pokeapi.model.PokeApiResource;
import io.github.lishangbu.avalon.shell.pokeapi.model.PokeApiTypeDetailResult;
import io.github.lishangbu.avalon.shell.pokeapi.service.PokeApiTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * POKE API属性服务实现
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@Service
public class PokeApiTypeServiceImpl implements PokeApiTypeService {
  private static final Logger logger = LoggerFactory.getLogger(PokeApiTypeServiceImpl.class);

  @Override
  public List<PokeApiResource> listPokeApiTypes() {
    RestClient restClient = RestClient.builder().baseUrl(PokeApiConstants.POKE_API_ENDPOINT).build();
    List<PokeApiResource> results = new ArrayList<>();
    PokeApiPagination<PokeApiResource> apiResult = requestRemoteType(restClient, "/type");
    // 循环处理分页数据
    while (apiResult != null && StringUtils.hasText(apiResult.next())) {
      // 异步等待1秒以避免频繁请求，优化资源使用
      try {
        results.addAll(apiResult.results());
        Thread.sleep(Duration.ofSeconds(1));
        logger.info("Fetched data, moving to next page: {}", apiResult.next());
        apiResult = requestRemoteType(restClient, apiResult.next());

      } catch (InterruptedException e) {
        logger.error("Error while waiting for next request", e);
        // 恢复中断状态
        Thread.currentThread().interrupt();
      }
    }
    // 最后的数据加入
    if (apiResult != null) {
      results.addAll(apiResult.results());
    }
    return results;
  }

  @Override
  public PokeApiTypeDetailResult getPokeApiTypeDetailByArg(String arg) {
    RestClient restClient = RestClient.builder().baseUrl(PokeApiConstants.POKE_API_ENDPOINT).build();
    String apiResultContent = restClient.get().uri("/type/{name}", arg).retrieve().body(String.class);
    return JsonUtils.readValue(apiResultContent, PokeApiTypeDetailResult.class);
  }


  /**
   * 请求远程API，获取Type数据
   */
  private PokeApiPagination<PokeApiResource> requestRemoteType(RestClient restClient, String url) {
    String apiResultContent = restClient.get().uri(url).retrieve().body(String.class);
    return JsonUtils.readValue(apiResultContent, new TypeReference<PokeApiPagination<PokeApiResource>>() {
    });
  }
}
