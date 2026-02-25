package io.github.lishangbu.avalon.pokeapi.util;

import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/// 命名资源工具类
///
/// 提供从 NamedApiResource 的 URL 中提取 ID 的辅助方法
///
/// @author lishangbu
/// @since 2025/8/9
@Slf4j
public abstract class NamedApiResourceUtils {
    // 静态初始化正则表达式和模式，避免重复编译
    private static final String API_DATA_TYPES_REGEX;
    private static final Pattern PATTERN;

    static {
        // 收集所有的 api data type 并生成正则表达式
        API_DATA_TYPES_REGEX =
                Arrays.stream(PokeDataTypeEnum.values())
                        .map(PokeDataTypeEnum::getType)
                        .collect(Collectors.joining("|"));
        String regex = "/(" + API_DATA_TYPES_REGEX + ")/(-?\\d+)/";
        PATTERN = Pattern.compile(regex);
    }

    public static Integer getId(NamedApiResource namedApiResource) {
        if (namedApiResource == null) {
            log.warn("NamedApiResource is null, cannot extract ID.");
            return null;
        }
        Matcher matcher = PATTERN.matcher(namedApiResource.url());

        if (matcher.find()) {
            try {
                // 提取数字并转换为整数
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                log.error("数字格式错误:[{}] ", e.getMessage());
                return null;
            }
        } else {
            log.warn("没有找到匹配的数字,URL不匹配预期格式: [{}]", namedApiResource.url());
            return null;
        }
    }
}
