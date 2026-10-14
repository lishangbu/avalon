package io.github.lishangbu.avalon.content.creature.model;

import java.util.Map;
import java.util.stream.Collectors;

/** 特性槽位与说明的唯一内容格式，目录及重放工具均深复制嵌套映射。 */
public record AbilitySlotsDefinition(Map<String, Map<String, String>> species, Map<String, String> abilities) {

	/** 调用方后续编辑原始映射不能改变已加载的槽位定义。 */
	public AbilitySlotsDefinition {
		species = species.entrySet()
			.stream()
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
		abilities = Map.copyOf(abilities);
	}
}
