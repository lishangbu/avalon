package io.github.lishangbu.avalon.content.text;

import io.github.lishangbu.avalon.content.ContentLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 纯内容规则测试：无需Spring或数据库，覆盖逃避匹配、配置失效及明确允许的边界。 */
class ForbiddenWordsCatalogTest {

	private final ForbiddenWordsCatalog catalog = new ForbiddenWordsCatalog();

	/** 使用人工给定的变形，不从实现规范化函数反算预期结果。 */
	@ParameterizedTest
	@ValueSource(strings = { "旅人官方客服", "官_方_客_服", "ＦｕＣｋ", "f u c k", "f\u00a0u\u3000c\tk", "f-u.c/k",
			"f\u200bu\u200dc\ufeffk", "f\u034fu\ufe0fc\uDB40\uDD00k", "f😀u★c￥k", "ＱＱ１２３", "abcADMINxyz", "明天充值",
			"官方管理員", "賣淫", "聯繫我", "草泥馬" })
	void commonEvasionsAndTraditionalEntriesAreBlocked(String text) {
		assertTrue(catalog.contains(text), text);
	}

	/** 不猜测拼音、相似字或任意音调；没有完整词条的普通游戏文字应允许。 */
	@ParameterizedTest
	@ValueSource(strings = { "旅人Ａ_09", "青芽", "晨露浏览器", "击杀獠牙兽王", "死亡后恢复", "wei xin", "fυck", "fúck", "未成年伙伴", "" })
	void normalTextAndUnpromisedTransliterationsRemainAllowed(String text) {
		assertFalse(catalog.contains(text), text);
		assertFalse(catalog.contains(null));
	}

	/** 配置中每个审核过的词条均有效，包含关系也应用于原词前后的正常字符。 */
	@Test
	void everyConfiguredEntryIsActiveWithoutRegexInterpretation() {
		var definition = ContentLoader.read("/content/forbidden-words.json", ForbiddenWordsCatalog.Definition.class);
		for (String word : definition.words())
			assertTrue(catalog.contains("前缀" + word + "后缀"), word);
		var literal = new ForbiddenWordsCatalog(new ForbiddenWordsCatalog.Definition(List.of("bad.*word")));
		assertTrue(literal.contains("bad_word"));
		assertFalse(literal.contains("badXXXXword"), "配置元字符不能变成可执行正则");
	}

	/** 修改构造输入不能在运行中切换词表，所有账号共享稳定的不可变启动快照。 */
	@Test
	void catalogDoesNotFollowLaterMutationsOfTheSourceList() {
		var source = new ArrayList<>(List.of("旧词"));
		var fixed = new ForbiddenWordsCatalog(new ForbiddenWordsCatalog.Definition(source));
		source.set(0, "新词");
		assertTrue(fixed.contains("旧词"));
		assertFalse(fixed.contains("新词"));
	}

	/** 空配置、不可见词条及等价重复必须阻止构造，不能默认为无拦截或全部拦截。 */
	@Test
	void emptyInvisibleAndNormalizedDuplicateEntriesFailClosed() {
		assertThrows(IllegalArgumentException.class, () -> new ForbiddenWordsCatalog(null));
		assertThrows(IllegalArgumentException.class,
				() -> new ForbiddenWordsCatalog(new ForbiddenWordsCatalog.Definition(null)));
		for (List<String> invalid : List.of(List.<String>of(), Arrays.asList((String) null), List.of(" "),
				List.of("_★\u200b"), List.of("x".repeat(81)), List.of("ADMIN", "ａｄｍｉｎ"), List.of("官_方", "官方"))) {
			var error = assertThrows(IllegalArgumentException.class,
					() -> new ForbiddenWordsCatalog(new ForbiddenWordsCatalog.Definition(invalid)));
			assertTrue(error.getMessage().contains("/content/forbidden-words.json"));
		}
	}

	/** 原始JSON仍由统一Schema约束；合法结构之后还必须经过上面的语义校验。 */
	@Test
	void schemaRejectsMissingEmptyWronglyTypedAndExtraFields() {
		var mapper = JsonMapper.builder().build();
		for (String json : List.of("{}", "{\"words\":[]}", "{\"words\":[\"\"]}",
				"{\"words\":[\" \t\"]}".replace("\t", "\\t"), "{\"words\":[null]}", "{\"words\":\"官方\"}",
				"{\"words\":[\"官方\",\"官方\"]}", "{\"words\":[\"官方\"],\"enabled\":false}")) {
			var document = (ObjectNode) mapper.readTree(json);
			document.put("$schema", "./schemas/forbidden-words.schema.json");
			assertThrows(IllegalArgumentException.class,
					() -> ContentLoader.validate("/content/forbidden-words.json", document));
		}
		assertDoesNotThrow(
				() -> ContentLoader.read("/content/forbidden-words.json", ForbiddenWordsCatalog.Definition.class));
	}

}
