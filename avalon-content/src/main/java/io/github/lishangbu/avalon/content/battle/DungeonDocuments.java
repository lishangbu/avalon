package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.model.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 一个副本文件同时定义关卡、奖励及宝箱；目录自动发现，不登记副本名称。 */
public final class DungeonDocuments {

	public record Bundle(DungeonDefinition dungeon, DungeonRewardDefinition rewards, DungeonChestDefinition chests) {
	}

	private static class Holder {

		private static final Map<String, Bundle> VALUE = load();

	}

	private DungeonDocuments() {
	}

	public static List<Bundle> current() {
		return List.copyOf(Holder.VALUE.values());
	}

	private static Map<String, Bundle> load() {
		return fromDocuments(ContentLoader.documents("dungeons"));
	}

	public static Map<String, Bundle> fromDocuments(Map<String, tools.jackson.databind.JsonNode> documents) {
		var result = new LinkedHashMap<String, Bundle>();
		var sources = new LinkedHashMap<String, String>();
		for (var entry : new java.util.TreeMap<>(documents).entrySet()) {
			String path = entry.getKey();
			ContentLoader.validate(path, entry.getValue());
			var bundle = ContentLoader.convert(entry.getValue(), Bundle.class);
			String id = bundle.dungeon().id();
			String previous = sources.putIfAbsent(id, path);
			if (previous != null)
				throw new IllegalArgumentException("副本编号重复 " + id + "：" + previous + "；" + path);
			if (!id.equals(bundle.chests().dungeon()))
				throw new IllegalArgumentException(path + " chests.dungeon：必须属于所在副本");
			result.put(id, bundle);
		}
		return java.util.Collections.unmodifiableMap(result);
	}

}
