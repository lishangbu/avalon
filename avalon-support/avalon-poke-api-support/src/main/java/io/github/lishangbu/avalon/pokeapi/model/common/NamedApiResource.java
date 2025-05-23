package io.github.lishangbu.avalon.pokeapi.model.common;

/**
 * 参考<a href="https://pokeapi.co/docs/v2#resource-listspagination-section">官网Resource
 * Lists/Pagination-named</a>
 *
 * @param name 引用资源的名称
 * @param url 引用资源的 URL
 * @author lishangbu
 * @since 2025/5/20
 */
public record NamedApiResource<T>(String name, String url) {}
