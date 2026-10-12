package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestPlacement;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestDeliveryRequirements;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 替补不能代替当前出战要求，普通同种个体不能代替指定来源和指定位置。 */
class QuestDeliveryRequirementsTest {

	@Test
	void partyAndSpecificPlacementAreIndependentRequirements() {
		var pets = mock(CreatureQueryService.class);
		var service = new QuestDeliveryRequirements(pets, mock(InventoryQueryService.class));
		var step = new QuestContentStep("TEST", "TEST", "编队", "SQUARE", "回报", "完成", List.of(), true, List.of(),
				new QuestPlacement("CAPTURE:LESSON", 1), List.of(), 2, false, null, false, List.of(), null);
		when(pets.list(1)).thenReturn(List.of(pet(0), pet(2), pet(100)));
		assertEquals("请先编入至少2只出战伙伴", service.missing(1, step));
		when(pets.list(1)).thenReturn(List.of(pet(0), pet(1)));
		assertNotNull(service.missing(1, step));
		when(pets.hasPlacement(1, "CAPTURE:LESSON", 1)).thenReturn(true);
		assertNull(service.missing(1, step));
	}

	private CreatureView pet(int position) {
		return new CreatureView("1", "CLAW_CAT", "利爪猫", "风", 1, 0, "A", "完好防护", List.of(), position, false);
	}

}
