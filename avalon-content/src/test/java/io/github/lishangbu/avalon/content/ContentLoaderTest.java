package io.github.lishangbu.avalon.content;

import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.model.MarketDefinition;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.world.model.WorldDefinition;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** 构建阶段执行与启动相同的 Schema 校验，并验证常见编辑错误不会进入领域构造器。 */
class ContentLoaderTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void itemCategoriesRequireMatchingAttributesAndRejectLegacyArrays() throws Exception {
		for (String field : new String[] { "category", "scope", "type", "recovery" }) {
			var source = document("items");
			var potion = (ObjectNode) source.path("items")
				.valueStream()
				.filter(i -> i.path("id").asString().equals("HEALTH_POTION"))
				.findFirst()
				.orElseThrow();
			potion.remove(field);
			assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/items.json", source));
		}
		for (String category : new String[] { "MATERIAL", "TOOL", "UNKNOWN" }) {
			var source = document("items");
			var potion = (ObjectNode) source.path("items")
				.valueStream()
				.filter(i -> i.path("id").asString().equals("HEALTH_POTION"))
				.findFirst()
				.orElseThrow();
			potion.put("category", category);
			assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/items.json", source));
		}
		var source = document("items");
		source.putArray("equipment");
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/items.json", source));
	}

	@Test
	void ownedDescriptionsMustBePresentAndContainVisibleText() throws Exception {
		for (var entry : Map.of("creatures", "pets", "evolution", "routes", "battle", "skills").entrySet()) {
			var source = document(entry.getKey());
			var item = (ObjectNode) source.path(entry.getValue()).get(0);
			item.remove("description");
			assertThrows(IllegalArgumentException.class,
					() -> ContentLoader.validate("/content/" + entry.getKey() + ".json", source));
			for (String invalid : new String[] { "", " \t\n" }) {
				item.put("description", invalid);
				assertThrows(IllegalArgumentException.class,
						() -> ContentLoader.validate("/content/" + entry.getKey() + ".json", source));
			}
		}
		var slots = document("ability-slots");
		((ObjectNode) slots.path("abilities")).put("灵爪", " \t");
		assertThrows(IllegalArgumentException.class,
				() -> ContentLoader.validate("/content/ability-slots.json", slots));
	}

	@Test
	void questSchemaRejectsUnknownFieldsAndMalformedRewards() throws Exception {
		for (String invalid : new String[] { "{\"kind\":\"GOLD\",\"amount\":0}",
				"{\"kind\":\"GIVE_ITEM\",\"amount\":1}",
				"{\"kind\":\"GIVE_PET\",\"reference\":\"CLAW_CAT\",\"amount\":2}",
				"{\"kind\":\"EXPERIENCE\",\"amount\":1,\"reference\":\"CLAW_CAT\"}",
				"{\"kind\":\"GOLD\",\"amount\":1,\"amout\":5}" }) {
			var quests = document("quests");
			((ObjectNode) quests.path("steps").get(0)).putArray("effects").add(MAPPER.readTree(invalid));
			assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/quests.json", quests));
		}
		var quests = document("quests");
		((ObjectNode) quests.path("steps").get(0)).put("minimunActive", 1);
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/quests.json", quests));
	}

	@Test
	void petGrowthCannotReintroduceOldCoefficientsAndHeroesStillRequireThem() throws Exception {
		var battle = document("battle");
		var pet = (ObjectNode) battle.path("growth").path("CLAW_CAT");
		pet.put("manaBase", 0);
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/battle.json", battle));
		pet.remove("manaBase");
		ContentLoader.validate("/content/battle.json", battle);
		((ObjectNode) battle.path("growth").path("WARRIOR")).remove("coefficients");
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/battle.json", battle));
	}

	@Test
	void creatureImageIsOptionalAndRestrictedToLocalAssets() throws Exception {
		var creatures = document("creatures");
		var pet = (ObjectNode) creatures.path("pets").get(0);
		assertEquals("/game-assets/creatures/claw-cat.webp", new CreatureCatalog().pet("CLAW_CAT").image());
		pet.remove("image");
		assertDoesNotThrow(() -> ContentLoader.validate("/content/creatures.json", creatures));
		for (String invalid : new String[] { "", "https://example.com/cat.webp",
				"/game-assets/creatures/../cat.webp" }) {
			pet.put("image", invalid);
			assertThrows(IllegalArgumentException.class,
					() -> ContentLoader.validate("/content/creatures.json", creatures));
		}
	}

	@Test
	void everyContentFileHasAValidSchemaAndNoLegacyYamlRemains() throws Exception {
		// 直接扫描源码资源，新增目录不会因为忘记维护测试名单而漏检。
		Path directory = Path.of("src/main/resources/content");
		try (var files = Files.list(directory)) {
			var documents = files.filter(Files::isRegularFile).toList();
			assertFalse(documents.isEmpty());
			for (Path document : documents) {
				assertTrue(document.toString().endsWith(".json"), document.toString());
				var loaded = ContentLoader.read("/content/" + document.getFileName(), JsonNode.class);
				assertNotNull(loaded);
				assertFalse(loaded.has("$schema"), "编辑器元数据不得传入领域对象");
			}
		}
	}

	@Test
	void everyHandwrittenSchemaConformsToDraft202012() throws Exception {
		var registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
		var metaSchema = registry.getSchema(SchemaLocation.of("https://json-schema.org/draft/2020-12/schema"));
		try (var files = Files.list(Path.of("src/main/resources/content/schemas"))) {
			for (Path file : files.toList()) {
				var errors = metaSchema.validate(MAPPER.readTree(file.toFile()));
				assertTrue(errors.isEmpty(), file + ": " + errors);
			}
		}
	}

	@Test
	void missingUnknownAndWronglyTypedFieldsFailWithFileAndField() throws Exception {
		assertInvalid("market", "{\"locations\":[\"CITY_SHOP\"]}", "playerLevel");
		assertInvalid("market", "{\"playerLevel\":16,\"locations\":[\"CITY_SHOP\"],\"plaeyrLevel\":16}", "plaeyrLevel");
		assertInvalid("market", "{\"playerLevel\":\"16\",\"locations\":[\"CITY_SHOP\"]}", "playerLevel");
		assertInvalid("market", "{\"playerLevel\":0,\"locations\":[\"CITY_SHOP\"]}", "playerLevel");
		assertInvalid("market", "{\"playerLevel\":16,\"locations\":[]}", "locations");
	}

	@Test
	void nestedEnumsSkillLimitsAndSharedAttributeNamesAreChecked() throws Exception {
		var creatures = document("creatures");
		var pet = (ObjectNode) creatures.path("pets").get(0);
		pet.put("abilitySlot", "Z");
		assertTrue(assertThrows(IllegalArgumentException.class,
				() -> ContentLoader.validate("/content/creatures.json", creatures))
			.getMessage()
			.contains("abilitySlot"));
		pet.put("abilitySlot", "A");
		var skills = pet.putArray("skills");
		for (int index = 0; index < 8; index++)
			skills.add("技能" + index);
		assertThrows(IllegalArgumentException.class,
				() -> ContentLoader.validate("/content/creatures.json", creatures));
		var items = document("items");
		((ObjectNode) items.path("items")
			.valueStream()
			.filter(i -> i.path("category").asString().equals("EQUIPMENT"))
			.findFirst()
			.orElseThrow()
			.path("equipment")).putObject("stats").put("physcialAttack", 3);
		assertTrue(
				assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/items.json", items))
					.getMessage()
					.contains("physcialAttack"));
	}

	@Test
	void schemaDoesNotReplaceCrossReferenceValidation() throws Exception {
		var document = document("opening");
		document.put("start", "DOES_NOT_EXIST");
		assertDoesNotThrow(() -> ContentLoader.validate("/content/opening.json", document));
		document.remove("$schema");
		var definition = MAPPER.convertValue(document, WorldDefinition.class);
		assertThrows(IllegalArgumentException.class, () -> WorldCatalog.validate(definition));
	}

	@Test
	void malformedJsonAndMissingResourcesNeverFallBackToDefaults() {
		assertThrows(IllegalStateException.class, () -> ContentLoader.read("/content/missing.json", JsonNode.class));
		var unregistered = assertThrows(IllegalStateException.class,
				() -> ContentLoader.read("/content/unregistered.json", JsonNode.class));
		assertTrue(unregistered.getMessage().contains("unregistered.schema.json"), unregistered.getMessage());
		var duplicate = assertThrows(IllegalStateException.class,
				() -> ContentLoader.read("/content/duplicate.json", JsonNode.class));
		assertInstanceOf(JacksonException.class, duplicate.getCause());
		var trailing = assertThrows(IllegalStateException.class,
				() -> ContentLoader.read("/content/trailing.json", JsonNode.class));
		assertInstanceOf(JacksonException.class, trailing.getCause());
		assertThrows(IllegalArgumentException.class,
				() -> ContentLoader.validate("/content/../market.json", MAPPER.createObjectNode()));
	}

	@Test
	void explicitSchemaMustMatchTheTrustedFilenameAndIsNotPartOfTheModel() throws Exception {
		var document = document("market");
		assertDoesNotThrow(() -> ContentLoader.validate("/content/market.json", document));
		for (String invalid : new String[] { "https://example.com/loose.schema.json", "./schemas/items.schema.json",
				"" }) {
			document.put("$schema", invalid);
			var error = assertThrows(IllegalArgumentException.class,
					() -> ContentLoader.validate("/content/market.json", document));
			assertTrue(error.getMessage().contains("$schema"), error.getMessage());
		}
		document.remove("$schema");
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/market.json", document));
		assertNotNull(ContentLoader.read("/content/market.json", MarketDefinition.class));
	}

	/** 负例使用原始文档保留 $schema；正式读取器返回的业务对象不含此元数据。 */
	private static ObjectNode document(String name) throws Exception {
		try (var input = ContentLoaderTest.class.getResourceAsStream("/content/" + name + ".json")) {
			return (ObjectNode) MAPPER.readTree(input);
		}
	}

	private static void assertInvalid(String name, String json, String field) throws Exception {
		ObjectNode document = (ObjectNode) MAPPER.readTree(json);
		document.put("$schema", "./schemas/" + name + ".schema.json");
		String resource = "/content/" + name + ".json";
		var error = assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate(resource, document));
		assertTrue(error.getMessage().contains(resource), error.getMessage());
		assertTrue(error.getMessage().contains(field), error.getMessage());
	}

}
