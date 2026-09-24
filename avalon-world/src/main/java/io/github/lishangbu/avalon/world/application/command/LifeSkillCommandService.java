package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRules.Skill;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 专业变更与回执共用角色锁，防止并发学习突破名额。 */
@Component
@RequiredArgsConstructor
public class LifeSkillCommandService {

	private final CharacterCommandService characters;

	private final LifeSkillRepository skills;

	private final InventoryCommandService inventory;

	@Transactional
	public void change(long owner, Request request, boolean forget) {
		if (request == null || request.skill() == null || request.requestId() == null)
			throw new BusinessException(400, "请选择生活技能并提供请求号");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, "请求号无效");
		}
		if (forget && !request.confirmed())
			throw new BusinessException(400, "遗忘会清空熟练度，请明确确认");
		var context = characters.lock(owner);
		String action = forget ? "FORGET" : "LEARN";
		String previous = skills.prior(context.id(), request.requestId());
		if (previous != null) {
			if (!previous.equals(action + ":" + request.skill().name()))
				throw new BusinessException(409, "请求号已用于其他专业操作");
			return;
		}
		characters.requireOutsideBattle(owner);
		characters.requireNoLifeActivity(owner);
		var owned = skills.owned(context.id());
		if (forget) {
			if (!owned.containsKey(request.skill()))
				throw new BusinessException(409, "尚未学习该技能");
			skills.forget(context.id(), request.skill());
		}
		else {
			if (context.level() < LifeSkillRules.LEARNING_LEVEL)
				throw new BusinessException(409, "人物达到6级后可以学习生活技能");
			if (owned.containsKey(request.skill()))
				throw new BusinessException(409, "已经学习该技能");
			if (request.skill().profession && owned.keySet()
				.stream()
				.filter(skill -> skill.profession)
				.count() >= LifeSkillRules.PROFESSION_LIMIT)
				throw new BusinessException(409, "最多学习三个生活专业；种植和钓鱼不占名额");
			skills.learn(context.id(), request.skill());
			// 每个技能首次教学赠送一次；遗忘后重学不再次发放，防止重复出售套利。
			inventory.grant(owner, "LIFE_TEACHING:" + request.skill().name(), "TOOL_WOOD_" + request.skill().tool, 1,
					false);
		}
		skills.record(context.id(), request.requestId(), action, request.skill());
	}

	public record Request(String requestId, Skill skill, boolean confirmed) {
	}

}
