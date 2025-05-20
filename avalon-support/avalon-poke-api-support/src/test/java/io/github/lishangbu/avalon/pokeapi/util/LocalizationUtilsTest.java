package io.github.lishangbu.avalon.pokeapi.util;

import static org.junit.jupiter.api.Assertions.*;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 本地化工具测试
 *
 * @author lishangbu
 * @since 2025/5/21
 */
class LocalizationUtilsTest {
  // 创建一个包含 Name 对象的列表
  private List<Name> names =
      Arrays.asList(
          new Name("简体中文", new NamedApiResource<>("zh-Hans", null)),
          new Name("繁体中文", new NamedApiResource<>("zh-Hant", null)),
          new Name("English", new NamedApiResource<>("en", null)));

  @Test
  void testGetLocalizationNameWithMatchingLocale() {

    // 调用 getLocalizationName 方法，查找英文
    Optional<Name> result = LocalizationUtils.getLocalizationName(names, "en");

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("English", result.get().name());
  }

  @Test
  void testGetLocalizationNameWithNoMatchingLocale() {
    // 调用 getLocalizationName 方法，查找一个不存在的语言
    Optional<Name> result = LocalizationUtils.getLocalizationName(names, "fr");

    // 验证结果
    assertFalse(result.isPresent());
  }

  @Test
  void testGetLocalizationNameWithEmptyNamesList() {
    // 创建一个空的名称列表
    List<Name> names = Arrays.asList();

    // 调用 getLocalizationName 方法
    Optional<Name> result = LocalizationUtils.getLocalizationName(names, "en");

    // 验证结果
    assertFalse(result.isPresent());
  }

  @Test
  void testGetLocalizationNameWithDefaultLocales() {

    // 调用 getLocalizationName 方法，没有传入 locales，使用默认 locales
    Optional<Name> result = LocalizationUtils.getLocalizationName(names);

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("简体中文", result.get().name());
  }

  @Test
  void testGetLocalizationNameWithMultipleMatchingLocales() {

    // 调用 getLocalizationName 方法，传入多个 locales
    Optional<Name> result = LocalizationUtils.getLocalizationName(names, "fr", "en", "zh-Hans");

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("English", result.get().name());
  }

  @Test
  void testGetLocalizationNameWithNullNames() {
    // 调用 getLocalizationName 方法，names 为 null
    Optional<Name> result = LocalizationUtils.getLocalizationName(null, "en");
    // 验证结果
    assertFalse(result.isPresent());
  }
}
