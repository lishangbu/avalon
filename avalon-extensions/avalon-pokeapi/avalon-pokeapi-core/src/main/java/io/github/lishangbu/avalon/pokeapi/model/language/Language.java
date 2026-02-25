package io.github.lishangbu.avalon.pokeapi.model.language;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import java.util.List;

/// 语言模型
///
/// 用于 API 资源的多语言支持
///
/// @param id       资源标识符
/// @param name     语言名称
/// @param official 游戏是否以此语言发布
/// @param iso639   两字母语言代码
/// @param iso3166  两字母国家代码
/// @param names    该资源在不同语言中的名称列表
/// @author lishangbu
/// @since 2025/5/20
public record Language(
        Integer id,
        String name,
        Boolean official,
        String iso639,
        String iso3166,
        List<Name> names) {}
