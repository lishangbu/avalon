package io.github.lishangbu.avalon.creature.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.content.creature.EvolutionCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.creature.persistence.mapper.CreatureStorageCodec;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService;
import io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService.Mode;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 进化报价不写业务数据；为保证个体与具体材料计划一致，短暂取得与提交相同的角色锁。 客户端只能选择材料模式，路线、等级、材料数量和结果绑定状态由服务端决定。
 */
@Component
@RequiredArgsConstructor
public class CreatureEvolutionPreviewService {

	private final CharacterCommandService characters;

	private final CharacterFactQueryService facts;

	private final CreatureRepository pets;

	private final CreatureStorageCodec codec;

	private final EvolutionCatalog catalog;

	private final WorldCatalog world;

	private final CultivationMaterialService materials;

	@Transactional
	public Preview preview(long owner, long id, Mode requestedMode) {
		var context = characters.requireOutsideDungeon(owner);
		characters.requireCreatureNotDispatched(owner, id);
		if (context.level() < catalog.definition().playerLevel())
			throw new BusinessException(409, "人物等级尚未达到进化教学要求");
		if (!world.provides(context.location(), "RECOVERY"))
			throw new BusinessException(409, "请到幻兽中心或学院疗养处等安全恢复地点进化");
		if (!facts.codes(owner).contains("EVOLUTION:TUTORIAL"))
			throw new BusinessException(409, "请先完成进化教学");
		var pet = pets.get(id);
		if (pet == null || !pet.getCharacterId().equals(context.id()) || pet.unavailable())
			throw new BusinessException(404, "伙伴不存在");
		var route = catalog.route(pet.getSpecies());
		if (route == null)
			throw new BusinessException(409, "该形态暂未开放进化");
		if (pet.getLevel() < route.level())
			throw new BusinessException(409, "伙伴等级不足");
		Mode mode = requestedMode == null ? (pet.isBound() ? Mode.BOUND_FIRST : Mode.UNBOUND_ONLY) : requestedMode;
		var plan = materials.preview(owner, route.materials(), mode);
		return new Preview(codec.view(pet), pet.isBound(), pet.isCustomName(), route.target(),
				pet.isCustomName() ? pet.getName() : route.name(), mode, plan, pet.isBound() || plan.bindsTarget());
	}

	public record Preview(CreatureView before, boolean bound, boolean customName, String target, String name, Mode mode,
			CultivationMaterialService.Plan materials, boolean resultBound) {
	}

}
