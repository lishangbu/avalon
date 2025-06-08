package io.github.lishangbu.avalon.pokeapi.util;

import static org.junit.jupiter.api.Assertions.*;

import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.Effect;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.common.VerboseEffect;
import io.github.lishangbu.avalon.pokeapi.model.common.VersionGroupFlavorText;
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

  // 创建一个包含 Effect 对象的列表
  private List<Effect> effects;

  // 创建一个包含 VerboseEffect 对象的列表
  private List<VerboseEffect> verboseEffects;

  // 创建一个包含 VersionGroupFlavorText 对象的列表
  private List<VersionGroupFlavorText> versionGroupFlavorTexts;

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

    // 初始化 effects 列表
    effects =
        Arrays.asList(
            new Effect("这是简体中文效果", new NamedApiResource<>("zh-Hans", null)),
            new Effect("這是繁體中文效果", new NamedApiResource<>("zh-Hant", null)),
            new Effect("This is English effect", new NamedApiResource<>("en", null)));

    // 初始化 verboseEffects 列表
    verboseEffects =
        Arrays.asList(
            new VerboseEffect("这是简体中文详细效果", "这是简体中文短效果", new NamedApiResource<>("zh-Hans", null)),
            new VerboseEffect("這是繁體中文詳細效果", "這是繁體中文短效果", new NamedApiResource<>("zh-Hant", null)),
            new VerboseEffect(
                "This is English verbose effect",
                "This is English short effect",
                new NamedApiResource<>("en", null)));

    // 初始化 versionGroupFlavorTexts 列表
    versionGroupFlavorTexts =
        Arrays.asList(
            new VersionGroupFlavorText(
                "这是简体中文版本组风味文本", new NamedApiResource<>("zh-Hans", null), null),
            new VersionGroupFlavorText(
                "這是繁體中文版本組風味文本", new NamedApiResource<>("zh-Hant", null), null),
            new VersionGroupFlavorText(
                "This is English version group flavor text",
                new NamedApiResource<>("en", null),
                null));
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
    // 调用 getLocalizationDescription 方法，查找��文描述
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

  // 以下是针对 getLocalizationEffect 方法的测试用例

  @Test
  void testGetLocalizationEffectWithMatchingLocale() {
    // 调用 getLocalizationEffect 方法，查找英��效果
    Optional<Effect> result = LocalizationUtils.getLocalizationEffect(effects, "en");

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("This is English effect", result.get().effect());
  }

  @Test
  void testGetLocalizationEffectWithNoMatchingLocale() {
    // 调用 getLocalizationEffect 方法，查找一个不存在的语言
    Optional<Effect> result = LocalizationUtils.getLocalizationEffect(effects, "fr");

    // 验证结果
    assertFalse(result.isPresent());
  }

  @Test
  void testGetLocalizationEffectWithEmptyList() {
    // 创建一个空的效果列表
    List<Effect> emptyEffects = Arrays.asList();

    // 调用 getLocalizationEffect 方法
    Optional<Effect> result = LocalizationUtils.getLocalizationEffect(emptyEffects, "en");

    // 验证结果
    assertFalse(result.isPresent());
  }

  @Test
  void testGetLocalizationEffectWithDefaultLocales() {
    // 调用 getLocalizationEffect 方法，使用默认语言顺序
    Optional<Effect> result = LocalizationUtils.getLocalizationEffect(effects);

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("这是简体中文效果", result.get().effect());
  }

  @Test
  void testGetLocalizationEffectWithMultipleMatchingLocales() {
    // 调用 getLocalizationEffect 方法，传入多个 locales
    Optional<Effect> result =
        LocalizationUtils.getLocalizationEffect(effects, "fr", "zh-Hant", "en");

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("這是繁體中文效果", result.get().effect());
  }

  @Test
  void testGetLocalizationEffectWithNullEffects() {
    // 调用 getLocalizationEffect 方法，effects 为 null
    Optional<Effect> result = LocalizationUtils.getLocalizationEffect(null, "en");

    // 验证结果
    assertFalse(result.isPresent());
  }

  // 以下是针对 getLocalizationVerboseEffect 方法的测试用例

  @Test
  void testGetLocalizationVerboseEffectWithMatchingLocale() {
    // 调用 getLocalizationVerboseEffect 方法，查找英文详细效果
    Optional<VerboseEffect> result =
        LocalizationUtils.getLocalizationVerboseEffect(verboseEffects, "en");

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("This is English verbose effect", result.get().effect());
    assertEquals("This is English short effect", result.get().shortEffect());
  }

  @Test
  void testGetLocalizationVerboseEffectWithNoMatchingLocale() {
    // 调用 getLocalizationVerboseEffect 方法，查找一个不存在的语言
    Optional<VerboseEffect> result =
        LocalizationUtils.getLocalizationVerboseEffect(verboseEffects, "fr");

    // 验证结果
    assertFalse(result.isPresent());
  }

  @Test
  void testGetLocalizationVerboseEffectWithEmptyList() {
    // 创建一个空的详细效果列表
    List<VerboseEffect> emptyVerboseEffects = Arrays.asList();

    // 调用 getLocalizationVerboseEffect 方法
    Optional<VerboseEffect> result =
        LocalizationUtils.getLocalizationVerboseEffect(emptyVerboseEffects, "en");

    // 验证结果
    assertFalse(result.isPresent());
  }

  @Test
  void testGetLocalizationVerboseEffectWithDefaultLocales() {
    // 调用 getLocalizationVerboseEffect 方法，使用默认语言顺序
    Optional<VerboseEffect> result = LocalizationUtils.getLocalizationVerboseEffect(verboseEffects);

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("这是简体中文详细效果", result.get().effect());
    assertEquals("这是简体中文短效果", result.get().shortEffect());
  }

  @Test
  void testGetLocalizationVerboseEffectWithMultipleMatchingLocales() {
    // 调用 getLocalizationVerboseEffect 方法，传入多个 locales
    Optional<VerboseEffect> result =
        LocalizationUtils.getLocalizationVerboseEffect(verboseEffects, "fr", "zh-Hant", "en");

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("這是繁體中文詳細效果", result.get().effect());
    assertEquals("這是繁體中文短效果", result.get().shortEffect());
  }

  @Test
  void testGetLocalizationVerboseEffectWithNullVerboseEffects() {
    // 调用 getLocalizationVerboseEffect 方法，verboseEffects 为 null
    Optional<VerboseEffect> result = LocalizationUtils.getLocalizationVerboseEffect(null, "en");

    // 验证结果
    assertFalse(result.isPresent());
  }

  // 以下是针对 getLocalizationVersionGroupFlavorText 方法的测试用例

  @Test
  void testGetLocalizationVersionGroupFlavorTextWithMatchingLocale() {
    // 调用 getLocalizationVersionGroupFlavorText 方法，查找英文版本组风味文本
    Optional<VersionGroupFlavorText> result =
        LocalizationUtils.getLocalizationVersionGroupFlavorText(versionGroupFlavorTexts, "en");

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("This is English version group flavor text", result.get().text());
  }

  @Test
  void testGetLocalizationVersionGroupFlavorTextWithNoMatchingLocale() {
    // 调用 getLocalizationVersionGroupFlavorText 方法，查找一个不存在的语言
    Optional<VersionGroupFlavorText> result =
        LocalizationUtils.getLocalizationVersionGroupFlavorText(versionGroupFlavorTexts, "fr");

    // 验证结果
    assertFalse(result.isPresent());
  }

  @Test
  void testGetLocalizationVersionGroupFlavorTextWithEmptyList() {
    // 创建一个空的版本组风味文本列表
    List<VersionGroupFlavorText> emptyVersionGroupFlavorTexts = Arrays.asList();

    // 调用 getLocalizationVersionGroupFlavorText 方法
    Optional<VersionGroupFlavorText> result =
        LocalizationUtils.getLocalizationVersionGroupFlavorText(emptyVersionGroupFlavorTexts, "en");

    // 验证结果
    assertFalse(result.isPresent());
  }

  @Test
  void testGetLocalizationVersionGroupFlavorTextWithDefaultLocales() {
    // 调用 getLocalizationVersionGroupFlavorText 方法，使用默认语言顺序
    Optional<VersionGroupFlavorText> result =
        LocalizationUtils.getLocalizationVersionGroupFlavorText(versionGroupFlavorTexts);

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("这是简体中文版本组风味文本", result.get().text());
  }

  @Test
  void testGetLocalizationVersionGroupFlavorTextWithMultipleMatchingLocales() {
    // 调用 getLocalizationVersionGroupFlavorText 方法，传入多个 locales
    Optional<VersionGroupFlavorText> result =
        LocalizationUtils.getLocalizationVersionGroupFlavorText(
            versionGroupFlavorTexts, "fr", "zh-Hant", "en");

    // 验证结果
    assertTrue(result.isPresent());
    assertEquals("這是繁體中文版本組風味文本", result.get().text());
  }

  @Test
  void testGetLocalizationVersionGroupFlavorTextWithNullVersionGroupFlavorTexts() {
    // 调用 getLocalizationVersionGroupFlavorText 方法，versionGroupFlavorTexts 为 null
    Optional<VersionGroupFlavorText> result =
        LocalizationUtils.getLocalizationVersionGroupFlavorText(null, "en");

    // 验证结果
    assertFalse(result.isPresent());
  }
}
