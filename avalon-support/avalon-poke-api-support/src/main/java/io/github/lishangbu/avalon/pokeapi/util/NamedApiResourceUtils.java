package io.github.lishangbu.avalon.pokeapi.util;

import io.github.lishangbu.avalon.pokeapi.enumeration.PokeApiEndpointEnum;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 命名资源工具类
 *
 * @author lishangbu
 * @since 2025/8/9
 */
public abstract class NamedApiResourceUtils {
  // 静态初始化正则表达式和模式，避免重复编译
  private static final String ENDPOINTS_REGEX;
  private static final Pattern PATTERN;

  static {
    // 收集所有的 endpoint 并生成正则表达式
    ENDPOINTS_REGEX =
        Arrays.stream(PokeApiEndpointEnum.values())
            .map(PokeApiEndpointEnum::getType)
            .collect(Collectors.joining("|"));
    String regex = "/(" + ENDPOINTS_REGEX + ")/(\\d+)/";
    PATTERN = Pattern.compile(regex);
  }

  public static Integer getId(NamedApiResource namedApiResource) {
    Matcher matcher = PATTERN.matcher(namedApiResource.url());

    if (matcher.find()) {
      try {
        // 提取数字并转换为整数
        return Integer.parseInt(matcher.group(2));
      } catch (NumberFormatException e) {
        System.out.println("数字格式错误: " + e.getMessage());
        return null;
      }
    } else {
      System.out.println("没有找到匹配的数字");
      return null;
    }
  }
}
