package io.github.lishangbu.avalon.content;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** 内容校验共用的诊断和只读索引；不缓存资源，不改变目录顺序。 */
public final class ContentChecks {

	private ContentChecks() {
	}

	public static void checkId(Set<String> ids, String id) {
		if (id == null || id.isBlank() || !ids.add(id))
			throw new IllegalArgumentException("内容编号重复或为空：" + id);
	}

	public static void checkId(String resource, String field, Set<String> ids, String id) {
		if (id == null || id.isBlank() || !ids.add(id))
			throw invalid(resource, id, field, id, "内容编号重复或为空");
	}

	public static <T> T required(String resource, String field, String id, T value) {
		if (value == null)
			throw invalid(resource, id, field, id, "引用不存在");
		return value;
	}

	public static IllegalArgumentException invalid(String resource, String id, String field, Object value,
			String reason) {
		return new IllegalArgumentException(resource + " [" + id + "] " + field + "=" + value + "：" + reason);
	}

	public static <T> Map<String, T> index(String resource, String field, List<T> values, Function<T, String> key) {
		var index = new LinkedHashMap<String, T>();
		for (var value : values) {
			String id = key.apply(value);
			if (id == null || id.isBlank() || index.putIfAbsent(id, value) != null)
				throw invalid(resource, id, field, id, "编号为空或重复");
		}
		return Collections.unmodifiableMap(index);
	}

}
