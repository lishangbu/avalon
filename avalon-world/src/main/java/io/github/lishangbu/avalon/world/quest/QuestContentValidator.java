package io.github.lishangbu.avalon.world.quest;

import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;
import io.github.lishangbu.avalon.quest.QuestDefinition;
import io.github.lishangbu.avalon.quest.QuestEngine;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 离线任务校验入口，不连接数据库、不启动Web服务、不调用资产命令。 使用生产目录和内置处理器的相同validate，校验Schema、引用、依赖图、规则与扩展。
 * 可给出上次部署导出的定义清单路径做兼容检查；清单只供部署审查，不作为运行定义版本。
 */
public final class QuestContentValidator {

	private QuestContentValidator() {
	}

	/** 无参数输出当前定义清单；提供基线JSON时先检查结构兼容，再输出当前清单。 */
	public static void main(String[] args) throws Exception {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var creatures = new CreatureCatalog();
		var quests = new QuestCatalog(world, items, creatures);
		var rules = new QuestRulesCatalog(quests);
		var builtins = new QuestBuiltinExtensions();
		// 离线过程仅调用validate，查询及资产依赖不会执行；不用构造数据库或伪造游戏状态。
		var extensions = new QuestExtensions(
				List.of(builtins.questExperience(null, null), builtins.questGold(null),
						builtins.questGiveItem(items, null), builtins.questTakeItem(items, null),
						builtins.questGivePet(creatures, null), builtins.questGiveEquipment(items, null, null)),
				List.of(builtins.questLocation(null, world), builtins.questReception(quests, null),
						builtins.questActivePartners(null), builtins.questPartnerPlacement(null),
						builtins.questFact(null), builtins.questInventory(items, null),
						builtins.questAcceptedStep(quests)));
		var catalog = new QuestEngineCatalog(quests, rules, extensions, items);
		var current = rules.definition().quests().stream().map(rule -> catalog.definition(rule.id())).toList();
		var json = JsonMapper.builder().build();
		if (args.length > 1)
			throw new IllegalArgumentException("用法：QuestContentValidator [上次部署定义清单.json]");
		if (args.length == 1) {
			var baseline = json.readValue(Files.readString(Path.of(args[0])), QuestDefinition[].class);
			for (var old : baseline) {
				var next = current.stream()
					.filter(value -> value.id().equals(old.id()))
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("不能删除已有任务定义：" + old.id()));
				new QuestEngine().compatible(old, next);
			}
		}
		System.out.println(json.writeValueAsString(current));
	}

}
