package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.creature.model.EvolutionRoute;
import io.github.lishangbu.avalon.content.creature.model.LearningBook;
import io.github.lishangbu.avalon.content.creature.model.LearningDefinition;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.model.ItemCategory;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 技能书和物种学习资格的启动期目录。书只提供学习机会，不授予池外资格；等级只读配置，不接收客户端覆盖。 战斗目录先完成技能效果校验，所以书不能引用尚未实现的技能。
 */
@Component
public class LearningCatalog {

	private final LearningDefinition definition;

	private final Map<String, LearningBook> byCode;

	private final Map<String, LearningBook> bySkill;

	public LearningCatalog(ItemCatalog items, CreatureCatalog creatures, BattleCatalog battle) {
		definition = ContentLoader.read("/content/creatures/learning.json", LearningDefinition.class);
		var books = items.ofCategory(ItemCategory.SKILL_BOOK)
			.stream()
			.map(i -> new LearningBook(i.id(), i.book().skill(), i.book().level()))
			.toList();
		Set<String> codes = new HashSet<>();
		Set<String> skills = new HashSet<>();
		for (var book : books) {
			ContentChecks.checkId(codes, book.code());
			if (!items.hasItem(book.code()) || !skills.add(book.skill()) || battle.skill(book.skill()) == null
					|| book.level() < 1 || book.level() > GrowthRules.LEVEL_CAP)
				throw new IllegalArgumentException("技能书配置无效：" + book.code());
		}
		definition.pools().forEach((species, pool) -> {
			if (!creatures.contains(species) || pool.isEmpty() || pool.size() != new HashSet<>(pool).size()
					|| !skills.containsAll(pool) || !pool.containsAll(creatures.pet(species).skills()))
				throw new IllegalArgumentException("物种技能池无效：" + species);
		});
		// 每一步都必须保留源形态全部可学资格；保留个体技能不应生成池外技能。
		var evolution = creatures.evolutionDefinition();
		validateEvolutionPools(definition.pools(), evolution.routes());
		byCode = books.stream().collect(Collectors.toUnmodifiableMap(LearningBook::code, b -> b));
		bySkill = books.stream().collect(Collectors.toUnmodifiableMap(LearningBook::skill, b -> b));
	}

	static void validateEvolutionPools(Map<String, List<String>> pools, List<EvolutionRoute> routes) {
		for (var route : routes) {
			var source = pools.get(route.source());
			var target = pools.get(route.target());
			if (source == null || target == null || !target.containsAll(source)
					|| (route.skill() != null && !target.contains(route.skill())))
				throw new IllegalArgumentException("进化学习池缺失或不继承源形态：" + route.source());
		}
	}

	public LearningBook book(String code) {
		return byCode.get(code);
	}

	public List<LearningBook> pool(String species) {
		return definition.pools().getOrDefault(species, List.of()).stream().map(bySkill::get).toList();
	}

}
