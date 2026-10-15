package io.github.lishangbu.avalon.world.application.life;

import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.domain.LifeToolRandom;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 生产公共内核：活动在取得角色锁后，为本次命令创建独立的专业与工具结算作业。 */
@Component
@RequiredArgsConstructor
public class LifeProductionKernel {

	private final LifeSkillRepository skills;

	private final LifeProductionCatalog catalog;

	private final InventoryCommandService items;

	private final LifeToolRandom random;

	/** 作业仅在调用者持锁事务内使用，不能跨请求保存或在事务外继续结算。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public LifeProductionWork open(long owner, long character, LifeSkillRulesSkill skill, int minimumLevel,
			String unavailableMessage) {
		Long points = skills.owned(character).get(skill);
		if (points == null || LifeSkillRules.level(points) < minimumLevel)
			throw new BusinessException(409, unavailableMessage);
		return new LifeProductionWork(owner, character, skill, points, skills, catalog, items, random);
	}

}
