package io.github.lishangbu.avalon.admin.mapstruct;

import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import jakarta.annotation.Resource;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// # MapStruct 基类
/// 提供公共的转换辅助方法，供各个 MapStruct 映射器复用
///
/// ## 主要职责
/// - 集中处理本地化名称的解析逻辑，避免重复实现
/// - 提供从 PokeAPI 实体或 {@code names} 列表解析本地化名称的通用工具方法
///
/// ## 设计要点
/// - 通过依赖注入 {@link  PokeApiService} 按需加载实体
/// - 使用反射安全地尝试解析 {@link  LocalizationUtils#getLocalizationName(List, String...)} 方法，失败时回退默认名称

public abstract class AbstractMapstruct {

  protected final Logger log = LoggerFactory.getLogger(this.getClass());

  @Resource protected PokeApiService pokeApiService;

  /// ## 名称解析
  /// 解析 PokeAPI 中的 {@code names} 列表获取本地化名称，失败时使用默认值
  ///
  /// @param names       PokeAPI 的 names 列表，可能为 null
  /// @param defaultName 默认名称，当无法解析本地化名称时使用
  /// @return 本地化名称或默认名称
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

  /// ## 资源名称解析
  /// 通过指定的数据类型和 {@code NamedApiResource} 获取实体并解析其 {@code names} 列表
  ///
  /// @param resource    资源引用，可能为 null
  /// @param dataType    数据类型枚举，用于告诉 PokeApiService 如何获取实体
  /// @param defaultName 默认名称，找不到本地化名称时使用
  /// @return 解析后的本地化名称或 defaultName
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
