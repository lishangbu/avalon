package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.content.life.model.LifeProductionTool;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.life.LifeProductionKernel;
import io.github.lishangbu.avalon.world.domain.LifeToolRandom;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 框架协议验证；真实产出、回执和事务回滚继续由各活动集成测试覆盖。 */
class LifeProductionKernelTest {

	private final LifeSkillRepository skills = mock(LifeSkillRepository.class);

	private final LifeProductionCatalog catalog = mock(LifeProductionCatalog.class);

	private final InventoryCommandService inventory = mock(InventoryCommandService.class);

	private final LifeToolRandom random = mock(LifeToolRandom.class);

	private final LifeProductionKernel kernel = new LifeProductionKernel(skills, catalog, inventory, random);

	private InventoryItemView tool(String code, int quantity) {
		// 与测试无关的交易和强化字段在夹具中显式填写，不再依赖生产模型的兼容默认值。
		return new InventoryItemView("30", "工具", code, quantity, null, 0, Map.of(), false, false, "普通", 0, 0, "冒险奖励",
				null, 0);
	}

	@Test
	void allExistingProfessionsUseTheSameToolAndRewardProtocol() {
		for (var skill : LifeSkillRulesSkill.values()) {
			when(skills.owned(20)).thenReturn(Map.of(skill, 0L));
			String code = "TOOL_WOOD_" + skill.tool;
			when(catalog.tool(code)).thenReturn(new LifeProductionTool(code, skill.tool, 0, 100));
			var work = kernel.open(10, 20, skill, 1, "技能不足");
			work.selectTool(tool(code, 2), 0, 1, "工具不足");
			assertFalse(work.complete(1, 1));
			assertEquals(2, work.halfPoints());
			assertEquals(1, work.toolsRemaining());
			verify(skills).progress(20, skill, 2);
		}
		verifyNoInteractions(inventory);
	}

	@Test
	void promotionDecaysTheNextRewardAndOnlySelectedToolsCanBeUsed() {
		when(skills.owned(20)).thenReturn(Map.of(LifeSkillRulesSkill.ARTISAN, 254L));
		when(catalog.tool("HAMMER")).thenReturn(new LifeProductionTool("HAMMER", "HAMMER", 0, 100));
		when(random.breaks(100)).thenReturn(true);
		var work = kernel.open(10, 20, LifeSkillRulesSkill.ARTISAN, 1, "技能不足");
		work.selectTool(tool("HAMMER", 5), 0, 2, "工具不足");
		assertTrue(work.complete(1, 3));
		assertEquals(2, work.level());
		assertEquals(260, work.halfPoints());
		assertTrue(work.complete(1, 3));
		assertEquals(263, work.halfPoints());
		assertEquals(0, work.toolsRemaining());
		assertThrows(IllegalStateException.class, () -> work.complete(1, 3));
		assertEquals(263, work.halfPoints());
		verify(inventory, times(2)).consume(10, 30, "HAMMER", 1);
		verify(random, times(2)).breaks(100);
		verify(skills).progress(20, LifeSkillRulesSkill.ARTISAN, 260);
		verify(skills).progress(20, LifeSkillRulesSkill.ARTISAN, 263);
		verifyNoMoreInteractions(inventory, random);
	}

	@Test
	void delayedProductionSeparatesPlantingWearFromHarvestRewards() {
		when(skills.owned(20)).thenReturn(Map.of(LifeSkillRulesSkill.FARMING, 0L));
		when(catalog.tool("HOE")).thenReturn(new LifeProductionTool("HOE", "HOE", 0, 100));
		var planting = kernel.open(10, 20, LifeSkillRulesSkill.FARMING, 1, "技能不足");
		planting.selectTool(tool("HOE", 1), 0, 1, "工具不足");
		when(random.breaks(100)).thenReturn(true);
		assertTrue(planting.wearTool());
		assertEquals(0, planting.halfPoints());
		verify(skills, never()).progress(anyLong(), any(), anyLong());
		clearInvocations(random, inventory);
		var harvest = kernel.open(10, 20, LifeSkillRulesSkill.FARMING, 1, "技能不足");
		harvest.reward(1, 4);
		assertEquals(8, harvest.halfPoints());
		assertEquals(0, planting.halfPoints(), "不同命令不能共享可变作业");
		verifyNoInteractions(random, inventory);
	}

	@Test
	void invalidQualificationsAndToolsNeverGrantRewardsOrConsumeInventory() {
		when(skills.owned(20)).thenReturn(Map.of());
		assertThrows(BusinessException.class, () -> kernel.open(10, 20, LifeSkillRulesSkill.MINING, 1, "技能不足"));
		when(skills.owned(20)).thenReturn(Map.of(LifeSkillRulesSkill.MINING, 0L));
		assertThrows(BusinessException.class, () -> kernel.open(10, 20, LifeSkillRulesSkill.MINING, 2, "技能不足"));
		var work = kernel.open(10, 20, LifeSkillRulesSkill.MINING, 1, "技能不足");
		when(catalog.tool("PICKAXE")).thenReturn(new LifeProductionTool("PICKAXE", "PICKAXE", 0, 100));
		when(catalog.tool("AXE")).thenReturn(new LifeProductionTool("AXE", "AXE", 0, 100));
		assertThrows(BusinessException.class, () -> work.selectTool(null, 0, 1, "工具不足"));
		assertThrows(BusinessException.class, () -> work.selectTool(tool("AXE", 1), 0, 1, "工具不足"));
		assertThrows(BusinessException.class, () -> work.selectTool(tool("PICKAXE", 1), 1, 1, "工具不足"));
		assertThrows(BusinessException.class, () -> work.selectTool(tool("PICKAXE", 1), 0, 2, "工具不足"));
		assertThrows(IllegalStateException.class, () -> work.complete(1, 1));
		work.selectTool(tool("PICKAXE", 1), 0, 1, "工具不足");
		assertThrows(IllegalStateException.class, () -> work.selectTool(tool("PICKAXE", 1), 0, 1, "工具不足"));
		assertThrows(IllegalArgumentException.class, () -> work.complete(2, 1));
		assertEquals(0, work.halfPoints());
		verify(skills, never()).progress(anyLong(), any(), anyLong());
		verifyNoInteractions(random, inventory);
	}

}
