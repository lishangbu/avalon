package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.creature.AbilitySlotsCatalog;
import io.github.lishangbu.avalon.content.creature.EvolutionCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionPreviewService;
import io.github.lishangbu.avalon.creature.application.command.EvolutionTeachingCommandService;
import io.github.lishangbu.avalon.creature.application.model.CreatureEvolutionRequest;
import io.github.lishangbu.avalon.creature.application.model.EvolutionTeachingRequest;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.model.CultivationMaterialMode;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 正式等级曲线内的首次进化事务；只准备等级和地点，不替换成长或材料规则。 */
class OpeningEvolutionTest extends SpringIntegrationTest {

	@Autowired
	AbilitySlotsCatalog abilities;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	GrowthRules growth;

	@Autowired
	EvolutionCatalog evolutionCatalog;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureRepository pets;

	@Autowired
	EvolutionTeachingCommandService teaching;

	@Autowired
	CreatureEvolutionPreviewService previews;

	@Autowired
	CreatureEvolutionCommandService evolution;

	@Autowired
	InventoryQueryService inventory;

	@Autowired
	EntityManager em;

	@ParameterizedTest
	@CsvSource({ "CLAW_CAT,STREAM_CLAW_CAT,溪爪猫,H,回春,水弹,微疗,WATER_PEARL,TIDAL_CLAW_CAT_ESSENCE,24",
			"SPROUT,BRANCH_SPIRIT,枝灵,H,回春,藤鞭,微疗,WOOD_SAP,ANCIENT_WOOD_GUARD_ESSENCE,24",
			"STONE_GOLEM,STONE_ARMOR_GOLEM,石甲怪,H,守护,碎岩击,掩护,EARTH_CORE,ROCK_GIANT_ESSENCE,24",
			"THUNDER_LIZARD,THUNDERCREST_LIZARD,雷冠蜥,H,雷怒,雷牙,掩护,THUNDER_CRYSTAL,THUNDERHORN_LIZARD_ESSENCE,24",
			"SHADOWTAIL_FOX,DUSKTAIL_FOX,暮尾狐,A,轻盈,暗影箭,微疗,SHADOW_DUST,NIGHTSHADOW_FOX_ESSENCE,20",
			"SHADOWTAIL_FOX,DUSKTAIL_FOX,暮尾狐,B,灵爪,暗影箭,微疗,SHADOW_DUST,NIGHTSHADOW_FOX_ESSENCE,20",
			"SHADOWTAIL_FOX,DUSKTAIL_FOX,暮尾狐,H,夜袭,暗影箭,微疗,SHADOW_DUST,NIGHTSHADOW_FOX_ESSENCE,20",
			"MOONLIGHT_MOTH,SILVERWING_MOTH,银翼蛾,A,完好防护,惩击,光幕,LIGHT_DUST,MOONHALO_MOTH_ESSENCE,23",
			"MOONLIGHT_MOTH,SILVERWING_MOTH,银翼蛾,B,完好防护,惩击,光幕,LIGHT_DUST,MOONHALO_MOTH_ESSENCE,23",
			"MOONLIGHT_MOTH,SILVERWING_MOTH,银翼蛾,H,明净,惩击,光幕,LIGHT_DUST,MOONHALO_MOTH_ESSENCE,23" })
	void openingEvolutionConsumesTeachingMaterialsAndRetainsIdentity(String source, String target, String targetName,
			String slot, String ability, String skillOne, String skillTwo, String common, String family,
			int finalLevel) {
		int firstLevel = source.equals("MOONLIGHT_MOTH") ? 15 : 12;
		String name = "cat_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Opening-evolution-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "MAGE");
		transactions.run(() -> characters.grantExperience(owner, Math.toIntExact(growth.threshold(firstLevel))));
		long id = transactions.call(() -> {
			long pet = creatures.capture(owner, "CAT_TEST", source, firstLevel - 1, slot, ability,
					List.of(skillOne, skillTwo));
			pets.get(pet).setExperience(1234);
			return pet;
		});
		transactions.run(() -> em.createNativeQuery("UPDATE player_character SET location='CITY_PETS' WHERE id=:id")
			.setParameter("id", characters.lock(owner).id())
			.executeUpdate());
		teaching.learn(owner, new EvolutionTeachingRequest(source, true));
		// 已学会进化后可返乡办理，不要求带着材料再次远行到诺姆研究设施。
		transactions.run(() -> em.createNativeQuery("UPDATE player_character SET location='PET_CENTER' WHERE id=:id")
			.setParameter("id", characters.lock(owner).id())
			.executeUpdate());
		assertThrows(BusinessException.class, () -> previews.preview(owner, id, CultivationMaterialMode.BOUND_FIRST));
		transactions.run(() -> pets.get(id).setLevel(firstLevel));
		var quote = previews.preview(owner, id, CultivationMaterialMode.BOUND_FIRST);
		assertEquals(target, quote.target());
		// 首次进化后保留中间形态培养窗口，最终形态按各家族配置开放。
		assertEquals(finalLevel, evolutionCatalog.route(target).level());
		var request = new CreatureEvolutionRequest(UUID.randomUUID().toString(), quote);
		var result = evolution.evolve(owner, id, request);
		assertEquals(result, evolution.evolve(owner, id, request));
		assertEquals(targetName, result.name());
		assertTrue(result.bound());
		transactions.run(() -> {
			var pet = pets.get(id);
			assertEquals(target, pet.getSpecies());
			assertEquals(slot, pet.getAbilitySlot());
			assertEquals(abilities.ability(target, slot), pet.getAbility());
			assertEquals(firstLevel, pet.getLevel());
			assertEquals(1234, pet.getExperience());
			assertEquals("[\"" + skillOne + "\",\"" + skillTwo + "\"]", pet.getSkills());
		});
		assertFalse(inventory.list(owner).stream().anyMatch(item -> List.of(common, family).contains(item.code())));
		assertThrows(BusinessException.class, () -> previews.preview(owner, id, CultivationMaterialMode.BOUND_FIRST));
		// 当前开放上限仍处于中间形态培养期，拒绝原因必须是等级而非恰好缺少材料。
		transactions.run(() -> pets.get(id).setLevel(15));
		var inventoryBefore = inventory.list(owner);
		var rejected = assertThrows(BusinessException.class,
				() -> previews.preview(owner, id, CultivationMaterialMode.BOUND_FIRST));
		assertEquals("伙伴等级不足", rejected.getMessage());
		assertEquals(inventoryBefore, inventory.list(owner));
		transactions.run(() -> {
			var pet = pets.get(id);
			assertEquals(target, pet.getSpecies());
			assertEquals(15, pet.getLevel());
			assertEquals(slot, pet.getAbilitySlot());
			assertEquals(1234, pet.getExperience());
			assertEquals("[\"" + skillOne + "\",\"" + skillTwo + "\"]", pet.getSkills());
		});
	}

}
