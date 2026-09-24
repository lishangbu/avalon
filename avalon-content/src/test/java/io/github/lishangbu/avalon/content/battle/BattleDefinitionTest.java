package io.github.lishangbu.avalon.content.battle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleDefinitionTest {

	@Test
	void nestedDefinitionIsIndependentOfMutableInputs() {
		var coefficients = new ArrayList<>(List.of(1.0, 2.0));
		var skills = new ArrayList<>(List.of("技能"));
		var growth = new BattleCatalog.Growth(coefficients, 1, 1, skills, 0, 0, 0, null, null);
		var growths = new HashMap<>(Map.of("HERO", growth));
		var multiplier = new HashMap<>(Map.of("火", 2.0));
		Map<String, Map<String, Double>> elements = new HashMap<>(Map.of("水", multiplier));
		var drops = new HashMap<>(Map.of("NONE", 100));
		var definition = new BattleCatalog.Definition(growths, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
				elements, drops, drops);
		coefficients.clear();
		skills.clear();
		growths.clear();
		multiplier.clear();
		elements.clear();
		drops.clear();
		assertEquals(List.of(1.0, 2.0), definition.growth().get("HERO").coefficients());
		assertEquals(List.of("技能"), growth.skills());
		assertEquals(2.0, definition.elements().get("水").get("火"));
		assertEquals(100, definition.supplyDrops().get("NONE"));
		assertEquals(100, definition.bookDrops().get("NONE"));
		assertThrows(UnsupportedOperationException.class, () -> growth.skills().clear());
		assertThrows(UnsupportedOperationException.class, () -> definition.elements().get("水").clear());
		assertThrows(UnsupportedOperationException.class, () -> definition.encounters().clear());
	}

	@Test
	void absentPetCoefficientsRemainDistinctFromConfiguredEmptyCoefficients() {
		assertNull(new BattleCatalog.Growth(null, 0, 0, List.of(), 1, 1, 2, "BOOK", "FRAGMENT").coefficients());
		assertNotNull(new BattleCatalog.Growth(List.of(), 0, 0, List.of(), 1, 1, 2, "BOOK", "FRAGMENT").coefficients());
	}

}
