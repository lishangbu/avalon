package io.github.lishangbu.avalon.pokeapi.util;

import io.github.lishangbu.avalon.pokeapi.model.common.*;
import io.github.lishangbu.avalon.pokeapi.model.move.MoveFlavorText;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.AbilityFlavorText;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/// 本地化工具
///
/// 提供从 PokeAPI 的本地化资源列表中按语言解析首选本地化资源的方法
///
/// @author lishangbu
/// @since 2025/5/21
public abstract class LocalizationUtils {

    /// 简体中文
    private static final String SIMPLIFIED_CHINESE = "zh-Hans";

    /// 繁体中文
    private static final String TRADITIONAL_CHINESE = "zh-Hant";

    /// 英文
    private static final String ENGLISH = "en";

    /// 默认按照简体中文、繁体中文和英语的顺序进行解析
    private static final String[] DEFAULT_LOCALES = {
        SIMPLIFIED_CHINESE, TRADITIONAL_CHINESE, ENGLISH
    };

    /// 获取本地化的名称
    ///
    /// @param names   语言资源列表
    /// @param locales 要查找的语言
    /// @return 本地化的名称
    public static Optional<Name> getLocalizationName(List<Name> names, String... locales) {
        return getLocalizedResource(names, Name::language, locales);
    }

    /// 获取本地化的描述
    ///
    /// @param descriptions 描述资源列表
    /// @param locales      要查找的语言
    /// @return 本地化的描述
    public static Optional<Description> getLocalizationDescription(
            List<Description> descriptions, String... locales) {
        return getLocalizedResource(descriptions, Description::language, locales);
    }

    /// 获取本地化的效果
    ///
    /// @param effects 效果资源列表
    /// @param locales 要查找的语言
    /// @return 本地化的效果名称
    public static Optional<Effect> getLocalizationEffect(List<Effect> effects, String... locales) {
        return getLocalizedResource(effects, Effect::language, locales);
    }

    /// 获取本地化的完整效果
    ///
    /// @param verboseEffects 完整效果资源列表
    /// @param locales        要查找的语言
    /// @return 本地化的完整效果名称
    public static Optional<VerboseEffect> getLocalizationVerboseEffect(
            List<VerboseEffect> verboseEffects, String... locales) {
        return getLocalizedResource(verboseEffects, VerboseEffect::language, locales);
    }

    /// 获取本地化的招式文本
    ///
    /// @param moveFlavorTexts 招式文本资源列表
    /// @param locales         要查找的语言
    /// @return 本地化的招式文本
    public static Optional<MoveFlavorText> getLocalizationMoveFlavorText(
            List<MoveFlavorText> moveFlavorTexts, String... locales) {
        return getLocalizedResource(moveFlavorTexts, MoveFlavorText::language, locales);
    }

    /// 获取本地化的版本组偏好文本
    ///
    /// @param versionGroupFlavorTexts 版本组偏好资源列表
    /// @param locales                 要查找的语言
    /// @return 本地化的版本组偏好文本
    public static Optional<VersionGroupFlavorText> getLocalizationVersionGroupFlavorText(
            List<VersionGroupFlavorText> versionGroupFlavorTexts, String... locales) {
        return getLocalizedResource(
                versionGroupFlavorTexts, VersionGroupFlavorText::language, locales);
    }

    /// 获取本地化的特性文本
    ///
    /// @param abilityFlavorTexts 特性文本资源列表
    /// @param locales            要查找的语言
    /// @return 本地化的特性文本
    public static Optional<AbilityFlavorText> getLocalizationAbilityFlavorText(
            List<AbilityFlavorText> abilityFlavorTexts, String... locales) {
        return getLocalizedResource(abilityFlavorTexts, AbilityFlavorText::language, locales);
    }

    /// 通用的本地化资源获取方法
    ///
    /// @param <T>               资源类型
    /// @param resources         资源列表
    /// @param languageExtractor 从资源中提取语言的函数
    /// @param locales           要查找的语言，如果为空则使用默认语言顺序
    /// @return 本地化的资源
    private static <T> Optional<T> getLocalizedResource(
            List<T> resources,
            Function<T, NamedApiResource<?>> languageExtractor,
            String... locales) {

        if (resources == null || resources.isEmpty()) {
            return Optional.empty();
        }

        // 如果没有传入 locales，使用默认的 locales
        String[] localesToUse = (locales.length == 0) ? DEFAULT_LOCALES : locales;

        // 遍历所有 locales，查找匹配的结果
        return Arrays.stream(localesToUse)
                .map(
                        locale ->
                                resources.stream()
                                        .filter(
                                                resource ->
                                                        languageExtractor
                                                                .apply(resource)
                                                                .name()
                                                                .equalsIgnoreCase(locale))
                                        .findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}
