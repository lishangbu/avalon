package io.github.lishangbu.avalon.pokeapi.model.pagination;

import io.github.lishangbu.avalon.pokeapi.model.common.APIResource;
import java.util.List;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Resource Lists/Pagination/APIResourceList</a>
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record APIResourceList(
    Integer count, String next, String previous, List<APIResource> results) {
  /** 获取此API可用资源的总数 */
  public Integer count() {
    return count;
  }

  /** 获取列表中下一页的URL */
  public String next() {
    return next;
  }

  /** 获取列表中上一页的URL */
  public String previous() {
    return previous;
  }

  /** 获取未命名的API资源列表 */
  public List<APIResource> results() {
    return results;
  }
}
