package io.github.lishangbu.avalon.identity.domain;

import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.*;

/** 只接受已开放的全局页面。固定的冒险和更多入口不能被偏好覆盖。 */
public final class NavigationRules {

	public static final List<String> DEFAULTS = List.of("COMPANIONS", "INVENTORY", "CHARACTER");

	private static final Set<String> ALLOWED = Set.of("COMPANIONS", "INVENTORY", "CHARACTER", "ENCYCLOPEDIA", "QUESTS");

	private NavigationRules() {
	}

	public static List<String> validate(List<String> slots) {
		if (slots == null || slots.size() != 3
				|| slots.stream().anyMatch(value -> value == null || !ALLOWED.contains(value))
				|| new HashSet<>(slots).size() != 3) {
			throw new BusinessException(400, "请选择三个不同的已开放导航入口");
		}
		return List.copyOf(slots);
	}

	/** 下线入口或旧档异常只修复读取投影，不在 GET 中修改账号；保留其余有效位置。 */
	public static List<String> restore(String stored) {
		String[] saved = stored == null ? new String[0] : stored.split(",", -1);
		List<String> result = new ArrayList<>(Arrays.asList(null, null, null));
		Set<String> used = new HashSet<>();
		for (int i = 0; i < Math.min(saved.length, 3); i++) {
			if (ALLOWED.contains(saved[i]) && used.add(saved[i]))
				result.set(i, saved[i]);
		}
		for (int i = 0; i < 3; i++) {
			if (result.get(i) != null)
				continue;
			for (String fallback : DEFAULTS) {
				if (used.add(fallback)) {
					result.set(i, fallback);
					break;
				}
			}
		}
		return List.copyOf(result);
	}

}
