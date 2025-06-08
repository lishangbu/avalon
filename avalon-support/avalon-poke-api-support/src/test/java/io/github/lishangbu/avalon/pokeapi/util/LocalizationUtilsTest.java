package io.github.lishangbu.avalon.pokeapi.util;

import static org.junit.jupiter.api.Assertions.*;

import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 本地化工具测试
 *
 * @author lishangbu
 * @since 2025/5/21
 */
class LocalizationUtilsTest {
  // 创建一个包含 Name 对象的列表
  private List<Name> names;

  // 创建一个包含 Description 对象的列表
  private List<Description> descriptions;

  @BeforeEach
  void setUp() {
    // 初始化 names 列表
    names =
        Arrays.asList(
            new Name("简体中文", new NamedApiResource<>("zh-Hans", null)),
            new Name("繁体中文", new NamedApiResource<>("zh-Hant", null)),
            new Name("English", new NamedApiResource<>("en", null)));

    // 初始化 descriptions 列表
    descriptions =
        Arrays.asList(
            new Description("这是简体中文描述", new NamedApiResource<>("zh-Hans", null)),
            new Description("這是繁體中文描述", new NamedApiResource<>("zh-Hant", null)),
            new Description("This is English description", new NamedApiResource<>("en", null)));
  }

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
    List<Name> emptyNames = Arrays.asList();

    // 调用 getLocalizationName 方法
    Optional<Name> result = LocalizationUtils.getLocalizationName(emptyNames, "en");

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

  // 以下是针对 getLocalizationDescription 方法的测试用例

  @Test
  void testGetLocalizationDescriptionWithMatchingLocale() {
    // 调用 getLocalizationDescription 方法，查找英文描述
    Optional<Description> result = LocalizationUtils.getLocalizationDescription(descriptions, "en");

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("This is English description", result.get().description());
  }

  @Test
  void testGetLocalizationDescriptionWithNoMatchingLocale() {
    // 调用 getLocalizationDescription 方法，查找一个不存在的语言
    Optional<Description> result = LocalizationUtils.getLocalizationDescription(descriptions, "fr");

    // 验证结果
    assertFalse(result.isPresent());
  }

  @Test
  void testGetLocalizationDescriptionWithEmptyList() {
    // 创建一个空的描述列表
    List<Description> emptyDescriptions = Arrays.asList();

    // 调用 getLocalizationDescription 方法
    Optional<Description> result =
        LocalizationUtils.getLocalizationDescription(emptyDescriptions, "en");

    // 验证结果
    assertFalse(result.isPresent());
  }

  @Test
  void testGetLocalizationDescriptionWithDefaultLocales() {
    // 调用 getLocalizationDescription 方法，使用默认语言顺序
    Optional<Description> result = LocalizationUtils.getLocalizationDescription(descriptions);

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("这是简体中文描述", result.get().description());
  }

  @Test
  void testGetLocalizationDescriptionWithMultipleMatchingLocales() {
    // 调用 getLocalizationDescription 方法，传入多个 locales
    Optional<Description> result =
        LocalizationUtils.getLocalizationDescription(descriptions, "fr", "zh-Hant", "en");

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("這是繁體中文描述", result.get().description());
  }

  @Test
  void testGetLocalizationDescriptionWithNullDescriptions() {
    // 调用 getLocalizationDescription 方法，descriptions 为 null
    Optional<Description> result = LocalizationUtils.getLocalizationDescription(null, "en");
    // 验证结果
    assertFalse(result.isPresent());
  }

  @Test
  void testCaseInsensitiveLanguageMatching() {
    // 测试不区分大小写的语言匹配
    Optional<Name> result = LocalizationUtils.getLocalizationName(names, "EN");

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("English", result.get().name());
  }

  @Test
  void testLocalesPriorityOrder() {
    // 测试语言优先级顺序，验证是否按照传入的顺序查找
    Optional<Name> result = LocalizationUtils.getLocalizationName(names, "en", "zh-Hans");

    // 应该返回英文，因为它在参数列表中排在前面
    assertTrue(result.isPresent());
    assertEquals("English", result.get().name());

    // 反转顺序，应该返回简体中文
    result = LocalizationUtils.getLocalizationName(names, "zh-Hans", "en");
    assertTrue(result.isPresent());
    assertEquals("简体中文", result.get().name());
  }
}
