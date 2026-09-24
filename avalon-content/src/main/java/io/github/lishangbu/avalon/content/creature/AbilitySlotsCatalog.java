package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.ContentChecks;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** 运行形态的特性槽位目录。生成抽槽位，进化保留槽位，两者再按目标物种读取同一目录。 */
@Component
public class AbilitySlotsCatalog {

	private final Map<String, Map<String, String>> species;

	private final Map<String, String> abilities;

	@Autowired
	public AbilitySlotsCatalog(CreatureCatalog creatures) {
		this(ContentLoader.read("/content/ability-slots.json", Definition.class), creatures);
	}

	public AbilitySlotsCatalog(Definition data, CreatureCatalog creatures) {
		abilities = Map.copyOf(data.abilities());
		data.species().forEach((id, slots) -> {
			if (!slots.keySet().equals(Set.of("A", "B", "H")))
				throw ContentChecks.invalid("/content/ability-slots.json", id, "species.slots", slots.keySet(),
						"必须恰好提供A/B/H槽位");
		});
		abilities.forEach((id, description) -> {
			if (id.isBlank() || description.isBlank())
				throw ContentChecks.invalid("/content/ability-slots.json", id, "abilities", description,
						"特性编号及说明不能为空白");
		});
		var referenced = data.species()
			.values()
			.stream()
			.flatMap(slots -> slots.values().stream())
			.collect(Collectors.toSet());
		checkCoverage("abilities", referenced, abilities.keySet());
		checkCoverage("species", creatures.all().stream().map(CreatureCatalog.Pet::id).collect(Collectors.toSet()),
				data.species().keySet());
		species = data.species()
			.entrySet()
			.stream()
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
	}

	private static void checkCoverage(String field, Set<String> expected, Set<String> actual) {
		var missing = new TreeSet<>(expected);
		missing.removeAll(actual);
		var extra = new TreeSet<>(actual);
		extra.removeAll(expected);
		if (!missing.isEmpty() || !extra.isEmpty())
			throw ContentChecks.invalid("/content/ability-slots.json", field, field, "缺少=" + missing + "，多余=" + extra,
					"必须完整覆盖引用");
	}

	/** 此方法不抽随机数；任何已有个体的槽位都不得因查询或进化重抽。 */
	public String ability(String id, String slot) {
		if (!Set.of("A", "B", "H").contains(slot) || !species.containsKey(id))
			throw new IllegalArgumentException("物种或特性槽位无效：" + id + "/" + slot);
		return species.get(id).get(slot);
	}

	public String description(String ability) {
		return abilities.get(ability);
	}

	record Definition(Map<String, Map<String, String>> species, Map<String, String> abilities) {
		Definition {
			species = species.entrySet()
				.stream()
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
			abilities = Map.copyOf(abilities);
		}
	}

}
