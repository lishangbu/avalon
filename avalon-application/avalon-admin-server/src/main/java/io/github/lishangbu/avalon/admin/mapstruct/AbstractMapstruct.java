package io.github.lishangbu.avalon.admin.mapstruct;

import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MapStruct 基类，提供公共的转换辅助方法
 *
 * <p>集中处理本地化名称的解析逻辑，避免在每个映射器中重复实现
 */
public abstract class AbstractMapstruct {

  protected final Logger log = LoggerFactory.getLogger(this.getClass());

  @Resource protected PokeApiService pokeApiService;

  /**
   * 从 PokeAPI 资源的 names 列表中解析本地化名称，失败时返回默认名称
   *
   * @param names PokeAPI 的 names 列表，可能为 null
   * @param defaultName 默认名称，当找不到本地化名称时使用
   * @return 本地化名称或默认名称
   */
  @SuppressWarnings("unchecked")
  protected String resolveLocalizedNameFromNames(List<?> names, String defaultName) {
    if (defaultName == null) return null;
    try {
      if (names == null) return defaultName;
      return LocalizationUtils.getLocalizationName((List<Name>) names)
          .map(Name::name)
          .orElse(defaultName);
    } catch (Exception e) {
      log.warn("Failed to resolve localized name, returning defaultName={}", defaultName, e);
      return defaultName;
    }
  }

  /**
   * 通过 NamedApiResource 和数据类型，从 PokeApiService 获取实体并解析其 names 列表
   *
   * @param resource 资源引用，可能为 null
   * @param dataType 数据类型枚举，用于告诉 PokeApiService 如何获取实体
   * @param defaultName 默认名称，找不到本地化名称时使用
   * @return 解析后的本地化名称或 defaultName
   */
  protected String resolveLocalizedNameFromResource(
      NamedApiResource<?> resource, PokeDataTypeEnum dataType, String defaultName) {
    if (defaultName == null) return null;
    try {
      if (resource == null) return defaultName;
      Object entity =
          pokeApiService.getEntityFromUri(dataType, NamedApiResourceUtils.getId(resource));
      if (entity == null) return defaultName;
      // try to reflectively get names() method
      try {
        java.lang.reflect.Method m = entity.getClass().getMethod("names");
        Object names = m.invoke(entity);
        if (names instanceof List) {
          return resolveLocalizedNameFromNames((List<?>) names, defaultName);
        }
      } catch (NoSuchMethodException ignored) {
        // entity has no names() method, fall back to default
      }
    } catch (Exception e) {
      log.warn("Failed to resolve localized name from resource, defaultName={}", defaultName, e);
    }
    return defaultName;
  }
}
