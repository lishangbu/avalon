package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRules.Skill;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService;
import io.github.lishangbu.avalon.world.application.model.FarmState;
import io.github.lishangbu.avalon.world.persistence.repository.FarmRepository;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 准备6级角色及独立生产夹具；装备夹具预置3级工匠，实际操作经页面、认证接口及隔离数据库执行。 */
@EnabledIfEnvironmentVariable(named = "AVALON_LIFE_BROWSER", matches = "1")
class LifeSkillsBrowserIT extends SpringIntegrationTest {

	@Autowired
	FarmRepository farms;

	@Autowired
	LifeSkillRepository proficiencies;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	LifeSkillCommandService skills;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	CombatVitalsStore vitals;

	@Autowired
	GrowthRules growth;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	QuestCatalog quests;

	@Autowired
	InventoryQueryService items;

	@Test
	void professionalSlotsAndForgetConfirmationWorkOnMobileAndDesktop() throws Exception {
		String prefix = "life_ui_" + UUID.randomUUID().toString().substring(0, 8);
		for (String device : new String[] { "desktop", "mobile" }) {
			prepareCooperation(prefix, device);
			String seller = prefix + "_market_" + device;
			registrations.register(seller, "Life-browser-1234", "测试");
			long sellerOwner = accounts.accountId(seller);
			characters.create(sellerOwner, "材料交易测试_" + device, "WARRIOR");
			transactions.run(() -> {
				characters.grantExperience(sellerOwner, Math.toIntExact(growth.threshold(11)));
				jdbc.sql("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
					.param("id", characters.lock(sellerOwner).id())
					.update();
				for (int index = 2; index <= 4; index++)
					inventory.grantStrict(sellerOwner, "MARKET_ORE:" + index, "LIFE_ORE_1", index, false, false);
				inventory.grantStrict(sellerOwner, "MARKET_BOUND", "LIFE_ORE_1", 99, false, true);
			});
			String fisher = prefix + "_fish_" + device;
			registrations.register(fisher, "Life-browser-1234", "测试");
			long fishingOwner = accounts.accountId(fisher);
			characters.create(fishingOwner, "钓鱼测试_" + device, "WARRIOR");
			transactions.run(() -> {
				characters.grantExperience(fishingOwner, 7380);
				inventory.grantWithBinding(fishingOwner, "FISH_BROWSER", "FISH_BAIT", 3, false, false);
				jdbc.sql("UPDATE player_character SET location='RIVERSIDE' WHERE id=:id")
					.param("id", characters.lock(fishingOwner).id())
					.update();
			});
			skills.change(fishingOwner,
					new LifeSkillCommandService.Request(UUID.randomUUID().toString(), Skill.FISHING, false), false);
			String farmer = prefix + "_farm_" + device;
			registrations.register(farmer, "Life-browser-1234", "测试");
			long farmingOwner = accounts.accountId(farmer);
			characters.create(farmingOwner, "种植测试_" + device, "WARRIOR");
			transactions.run(() -> {
				characters.grantExperience(farmingOwner, 7380);
				characters.changeGold(farmingOwner, 200);
				inventory.grantWithBinding(farmingOwner, "FARM_SEED", "SEED_0_0", 2, false, false);
				long character = characters.lock(farmingOwner).id();
				inventory.grantWithBinding(farmingOwner, "FOOD_BROWSER", "FOOD_0_0", 2, false, false);
				inventory.grantWithBinding(farmingOwner, "NUMBERED_POTION", "LIFE_HEALTH_POTION_1", 1, false, false);
				vitals.saveVitals(character, character, 1, 0);
				jdbc.sql("UPDATE player_character SET location='SQUARE' WHERE id=:id").param("id", character).update();
				farms.save(character, new FarmState(6,
						Map.of(2, new FarmState.Crop("CROP_0_0", false, Instant.now().minusSeconds(1000)))));
			});
			skills.change(farmingOwner,
					new LifeSkillCommandService.Request(UUID.randomUUID().toString(), Skill.FARMING, false), false);
			String username = prefix + "_" + device;
			registrations.register(username, "Life-browser-1234", "测试");
			long owner = accounts.accountId(username);
			characters.create(owner, "生活测试_" + device, "WARRIOR");
			transactions.run(() -> characters.grantExperience(owner, 7380));
			String maker = prefix + "_craft_" + device;
			registrations.register(maker, "Life-browser-1234", "测试");
			long craftingOwner = accounts.accountId(maker);
			characters.create(craftingOwner, "制造测试_" + device, "WARRIOR");
			transactions.run(() -> {
				characters.grantExperience(craftingOwner, 7380);
				jdbc.sql("UPDATE player_character SET location='GNOME_FORGE' WHERE id=:id")
					.param("id", characters.lock(craftingOwner).id())
					.update();
				inventory.grantWithBinding(craftingOwner, "BROWSER_ORE", "LIFE_ORE_0", 6, false, false);
			});
			skills.change(craftingOwner,
					new LifeSkillCommandService.Request(UUID.randomUUID().toString(), Skill.ARTISAN, false), false);
			String gearMaker = prefix + "_gear_" + device;
			registrations.register(gearMaker, "Life-browser-1234", "测试");
			long gearOwner = accounts.accountId(gearMaker);
			characters.create(gearOwner, "装备测试_" + device, "WARRIOR");
			transactions.run(() -> characters.grantExperience(gearOwner, 7380));
			skills.change(gearOwner,
					new LifeSkillCommandService.Request(UUID.randomUUID().toString(), Skill.ARTISAN, false), false);
			transactions.run(() -> {
				long id = characters.lock(gearOwner).id();
				jdbc.sql("UPDATE player_character SET location='GNOME_FORGE' WHERE id=:id").param("id", id).update();
				for (int index = 0; index < 192; index++)
					inventory.grantStrict(gearOwner, "GEAR_ORE:" + index, "LIFE_ORE_1", 1, false, index >= 99);
				for (int index = 0; index < 96; index++)
					inventory.grantStrict(gearOwner, "GEAR_WOOD:" + index, "LIFE_WOOD_1", 1, false, false);
				inventory.grantStrict(gearOwner, "GEAR_HAMMER", "TOOL_COPPER_HAMMER", 1, false, false);
				proficiencies.progress(id, Skill.ARTISAN, 768);
			});
			String gatherer = prefix + "_gather_" + device;
			registrations.register(gatherer, "Life-browser-1234", "测试");
			long gatheringOwner = accounts.accountId(gatherer);
			characters.create(gatheringOwner, "采集测试_" + device, "WARRIOR");
			transactions.run(() -> {
				characters.grantExperience(gatheringOwner, 7380);
				creatures.grant(gatheringOwner, "GATHER_BROWSER", "SPROUT");
				inventory.grantWithBinding(gatheringOwner, "GATHER_HP", "HEALTH_POTION", 2, false, false);
				inventory.grantWithBinding(gatheringOwner, "GATHER_MP", "MANA_POTION", 2, false, false);
				vitals.saveVitals(characters.lock(gatheringOwner).id(),
						Long.parseLong(pets.list(gatheringOwner).getFirst().id()), 5, 0);
				jdbc.sql("UPDATE player_character SET location='CAVE_ENTRANCE' WHERE id=:id")
					.param("id", characters.lock(gatheringOwner).id())
					.update();
			});
			skills.change(gatheringOwner,
					new LifeSkillCommandService.Request(UUID.randomUUID().toString(), Skill.MINING, false), false);
		}
		Path root = Path.of("").toAbsolutePath().getParent();
		Path log = root.resolve("avalon-app/target/life-browser.log");
		var builder = new ProcessBuilder("npm", "run", "test:e2e", "--", "life-skills.spec.ts", "--workers=1")
			.directory(root.resolve("avalon-ui").toFile())
			.redirectErrorStream(true)
			.redirectOutput(log.toFile());
		String filter = System.getProperty("avalon.life.browser.grep");
		if (filter != null && !filter.isBlank()) {
			builder.command().add("--grep");
			builder.command().add(filter);
		}
		builder.environment().put("AVALON_LIFE_USER", prefix);
		try (var packaged = new PackagedGameProcess(DATABASE.getJdbcUrl(), DATABASE.getUsername(),
				DATABASE.getPassword(), root.resolve("avalon-app/target/life-packaged.log"))) {
			builder.environment().put("AVALON_BASE_URL", packaged.baseUrl);
			var process = builder.start();
			try {
				assertTrue(process.waitFor(240, TimeUnit.SECONDS), "浏览器超时，见 " + log);
				assertEquals(0, process.exitValue(), "浏览器失败，见 " + log);
			}
			finally {
				if (process.isAlive()) {
					process.descendants().forEach(ProcessHandle::destroy);
					process.destroyForcibly();
				}
			}
		}
	}

	/** 只预置成长、剧情、工具和资金；交易制造所用碎石必须由浏览器实际采得。 */
	private void prepareCooperation(String prefix, String device) {
		for (String role : new String[] { "miner", "buyer", "poor", "full" }) {
			String username = prefix + "_c_" + role + "_" + device;
			registrations.register(username, "Life-browser-1234", "测试");
			long owner = accounts.accountId(username);
			characters.create(owner, "合作_" + role + "_" + device, "WARRIOR");
			transactions.run(() -> characters.grantExperience(owner, Math.toIntExact(growth.threshold(11))));
			Skill skill = role.equals("miner") ? Skill.MINING : Skill.ARTISAN;
			skills.change(owner, new LifeSkillCommandService.Request(UUID.randomUUID().toString(), skill, false),
					false);
			transactions.run(() -> {
				long id = characters.lock(owner).id();
				quests.definition().steps().forEach(step -> {
					facts.record(owner, "QUEST:" + step.id());
					step.facts().forEach(fact -> facts.record(owner, fact));
				});

				if (role.equals("miner")) {
					proficiencies.progress(id, skill, LifeSkillRules.threshold(2) * 2);
					creatures.grant(owner, "CHAIN_PET", "SPROUT");
					inventory.grantStrict(owner, "CHAIN_RESERVE", "TOOL_WOOD_PICKAXE", 10, false, false);
				}
				if (role.equals("full")) {
					for (int index = 0; items.bag(owner).usedSlots() < items.bag(owner).capacity(); index++)
						inventory.grantStrict(owner, "CHAIN_FILL:" + index, "STARTER_WEAPON", 1, true, true);
				}
			});
			jdbc.sql("UPDATE player_character SET gold=:gold, location=:location WHERE account_id=:id")
				.param("id", accounts.accountId(username))
				.param("gold", role.equals("poor") || role.equals("miner") ? 0 : 1000)
				.param("location", role.equals("miner") ? "GNOME_MINE_GATE" : "CITY_SHOP")
				.update();
		}
	}

}
