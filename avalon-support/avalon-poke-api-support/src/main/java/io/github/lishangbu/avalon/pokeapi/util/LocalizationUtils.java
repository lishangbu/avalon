package io.github.lishangbu.avalon.pokeapi.util;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 本地化工具
 *
 * @author lishangbu
 * @since 2025/5/21
 */
public abstract class LocalizationUtils {

  /** 简体中文 */
  public static final String SIMPLIFIED_CHINESE = "zh-Hans";

  /** 繁体中文 */
  public static final String TRADITIONAL_CHINESE = "zh-Hant";

  /** 英文 */
  public static final String ENGLISH = "en";

  /** 默认按照简体中文，繁体中文和英语的顺序进行解析 */
  private static final String[] DEFAULT_LOCALES = {
    SIMPLIFIED_CHINESE, TRADITIONAL_CHINESE, ENGLISH
  };

  /**
   * 获取本地化的语言名称
   *
   * @param names 语言资源列表
   * @param locales 要查找的语言
   * @return 本地化的语言名称
   */
  public static Optional<Name> getLocalizationName(List<Name> names, String... locales) {
    if (names == null || names.isEmpty()) {
      return Optional.empty();
    }

    // 如果没有传入 locales，使用默认的 locales
    if (locales.length == 0) {
      locales = DEFAULT_LOCALES;
    }

    // 遍历所有 locales，查找匹配的结果
    return Arrays.stream(locales)
        .map(
            locale ->
                names.stream()
                    .filter(name -> name.language().name().equalsIgnoreCase(locale))
                    .findFirst())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }
}
