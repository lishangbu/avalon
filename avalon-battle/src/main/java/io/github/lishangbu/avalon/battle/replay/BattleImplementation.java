package io.github.lishangbu.avalon.battle.replay;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.combat.CombatRoundEngine;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.quest.model.QuestEffect;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** 实际执行字节码的摘要，不保留旧执行器；历史记录必须使用相同提交及JDK构建的工具。 */
public final class BattleImplementation {

	/** 启动时计算一次，运行过程中不支持替换实现。 */
	private static final String CURRENT = compute();

	private BattleImplementation() {
	}

	/** 日志和重放工具使用同一实现标识，内容快照单独记录。 */
	public static String current() {
		return CURRENT;
	}

	/** 稳定排序的资源摘要同时适用于classes目录、普通JAR及生产嵌套JAR。 */
	private static String compute() {
		var digest = BattleReplayCodec.sha256();
		digest.update(System.getProperty("java.runtime.version").getBytes(StandardCharsets.UTF_8));
		var entries = new TreeMap<String, byte[]>();
		// 资源根按模块限制，避免将数据库驱动或宿主依赖误认为战斗规则。
		var roots = Map.of(CombatRoundEngine.class, "io/github/lishangbu/avalon/combat/", BattleEngine.class,
				"io/github/lishangbu/avalon/battle/", ContentLoader.class, "io/github/lishangbu/avalon/content/",
				QuestEffect.class, "io/github/lishangbu/avalon/quest/");
		for (var module : roots.entrySet()) {
			try {
				var resolver = new PathMatchingResourcePatternResolver(module.getKey().getClassLoader());
				var resources = resolver.getResources("classpath*:" + module.getValue() + "**/*.class");
				if (resources.length == 0)
					throw new IllegalStateException("战斗实现字节码目录为空：" + module.getValue());
				for (var resource : resources) {
					String url = resource.getURL().toExternalForm();
					String name = url.substring(url.lastIndexOf(module.getValue()));
					try (var input = resource.getInputStream()) {
						if (entries.putIfAbsent(name, input.readAllBytes()) != null)
							throw new IllegalStateException("战斗实现存在重复类：" + name);
					}
				}
			}
			catch (Exception exception) {
				throw new IllegalStateException("无法读取战斗实现字节码：" + module.getValue(), exception);
			}
		}
		entries.forEach((name, bytes) -> {
			digest.update(name.getBytes(StandardCharsets.UTF_8));
			digest.update((byte) 0);
			digest.update(bytes);
		});
		return HexFormat.of().formatHex(digest.digest());
	}

}
