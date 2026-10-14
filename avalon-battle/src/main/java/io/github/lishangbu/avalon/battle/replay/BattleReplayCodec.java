package io.github.lishangbu.avalon.battle.replay;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** 战斗文本记录的严格编解码与摘要；属性和映射稳定排序，不依赖HashMap遍历顺序。 */
public final class BattleReplayCodec {

	/** 缺失原语、未知或忽略字段及尾随文档均拒绝，旧存档字段不会静默丢失。 */
	private static final JsonMapper JSON = JsonMapper.builder()
		.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
		.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
		.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
		.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		.enable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
		.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
		.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
		.build();

	private BattleReplayCodec() {
	}

	/** 每个对象生成一行JSON，字符串中的换行由编码器转义。 */
	public static String write(Object value) {
		return JSON.writeValueAsString(value);
	}

	/** 正式重放只接受唯一新格式，解析异常向外传播而不补默认内容。 */
	public static <T> T read(String text, Class<T> type) {
		return JSON.readValue(text, type);
	}

	/** 固定UTF-8摘要用于内容、完整回合及链式记录完整性校验。 */
	public static String digest(String text) {
		return HexFormat.of().formatHex(sha256().digest(text.getBytes(StandardCharsets.UTF_8)));
	}

	/** JDK标准算法必定存在；异常仍明确抛出而不替换成不稳定的hashCode。 */
	static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("当前JDK缺少SHA-256", exception);
		}
	}

}
