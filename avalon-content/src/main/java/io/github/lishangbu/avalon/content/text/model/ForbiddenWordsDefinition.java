package io.github.lishangbu.avalon.content.text.model;

import java.util.List;

/** 原始配置结构；加载后转成只读检测集合，不向客户端提供词表查询接口。 */
public record ForbiddenWordsDefinition(List<String> words) {
}
