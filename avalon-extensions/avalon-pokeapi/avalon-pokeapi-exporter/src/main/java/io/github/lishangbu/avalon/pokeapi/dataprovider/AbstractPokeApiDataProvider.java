package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.LocalizationUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

/// 抽象的PokeApi数据提供接口
///
/// @author lishangbu
/// @since 2026/2/4
@Slf4j
public abstract class AbstractPokeApiDataProvider<R, T>
    implements PokeApiDataProvider<T>, PokeApiDataConverter<R, T> {
  @Autowired protected PokeApiService pokeApiService;

  @Override
  public List<T> fetch(PokeDataTypeEnum typeEnum, Class<T> type) {
    NamedAPIResourceList resourceList = pokeApiService.listNamedAPIResources(typeEnum);
    @SuppressWarnings("unchecked")
    List<R> pokeApiDatas =
        resourceList.results().stream()
            .map(
                namedApiResource ->
                    (R)
                        pokeApiService.getEntityFromUri(
                            typeEnum, NamedApiResourceUtils.getId(namedApiResource)))
            .collect(Collectors.toList());
    if (CollectionUtils.isEmpty(pokeApiDatas)) {
      return Collections.emptyList();
    } else {
      return pokeApiDatas.stream()
          .filter(Objects::nonNull)
          .map(this::convert)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }
  }

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
}
