package io.github.lishangbu.avalon.world.quest;

import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestRule;
import io.github.lishangbu.avalon.quest.QuestDefinition;
import io.github.lishangbu.avalon.quest.QuestEngine;
import io.github.lishangbu.avalon.world.persistence.entity.QuestDefinitionGuard;
import io.github.lishangbu.avalon.world.persistence.repository.QuestDefinitionGuardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * 部署时将新目录与上一次成功部署的结构比较；不兼容变更阻止应用启动。 数据库存的是单份结构基线，不给运行时使用，也不产生定义版本或多份执行快照。
 * 已注册但从当前内容删除的任务同样拒绝，避免存量实例无法完成。
 */
@Component
@RequiredArgsConstructor
public class QuestDeploymentGuard implements SmartInitializingSingleton {

	private final QuestDefinitionGuardRepository repository;

	private final QuestEngineCatalog catalog;

	private final QuestRulesCatalog rules;

	private final ObjectMapper json;

	private final PlatformTransactionManager transactionManager;

	/** Spring所有普通单例完成后运行，数据库迁移及目录校验此时已完成。 */
	@Override
	public void afterSingletonsInstantiated() {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			var ids = rules.definition().quests().stream().map(QuestRule::id).toList();
			for (var old : repository.findAll())
				if (!ids.contains(old.getId()))
					throw new IllegalArgumentException("不能删除已有任务定义：" + old.getId());
			for (String id : ids) {
				var next = catalog.definition(id);
				var row = repository.findById(id).orElse(null);
				if (row == null) {
					row = new QuestDefinitionGuard();
					row.setId(id);
				}
				else
					new QuestEngine().compatible(json.readValue(row.getDefinition(), QuestDefinition.class), next);
				row.setDefinition(json.writeValueAsString(next));
				repository.save(row);
			}
			repository.flush();
		});
	}

}
