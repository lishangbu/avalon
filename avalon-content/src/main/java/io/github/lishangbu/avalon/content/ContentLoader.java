package io.github.lishangbu.avalon.content;

import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 启动内容读取入口。解析失败包含文件路径，禁止静默回退默认内容。
 */
public final class ContentLoader {

	/** 严格解析标准 JSON：重复字段和尾随第二份文档均视为错误，不静默覆盖内容。 */
	private static final ObjectMapper MAPPER = JsonMapper.builder()
		.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
		.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
		// Schema 决定必填字段；可选原语保持既有 false/0 默认语义。
		.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
		.build();

	/** Schema 与内容一起打包；仅从本地资源解析引用，不允许启动时访问远程服务器。 */
	private static final SchemaRegistry SCHEMAS = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12,
			builder -> builder.schemas(location -> {
				if (!location.startsWith("classpath:content/schemas/") || location.contains(".."))
					throw new IllegalArgumentException("不允许外部 Schema 引用：" + location);
				String resource = "/" + location.substring("classpath:".length());
				try (var input = ContentLoader.class.getResourceAsStream(resource)) {
					if (input == null)
						throw new IllegalArgumentException("缺少 Schema：" + resource);
					return new String(input.readAllBytes(), StandardCharsets.UTF_8);
				}
				catch (IOException error) {
					throw new IllegalStateException("Schema 读取失败：" + resource, error);
				}
			}));

	private ContentLoader() {
	}

	/**
	 * 以指定记录类型解析资源，文件不存在或格式不合法则带资源路径抛启动错误。调用方继续做业务引用校验；读取器不提供热更新、内容版本或默认空目录回退。
	 */
	public static <T> T read(String resource, Class<T> type) {
		try (var input = ContentLoader.class.getResourceAsStream(resource)) {
			if (input == null)
				throw new IllegalArgumentException("缺少内容文件：" + resource);
			JsonNode document = MAPPER.readTree(input);
			validate(resource, document);
			// 编辑器元数据已由固定 Schema 核对路径；校验通过后移除，避免传入业务记录。
			// 所有内容 Schema 均要求根节点为对象，因此只在结构校验成功后转换。
			((ObjectNode) document).remove("$schema");
			return MAPPER.treeToValue(document, type);
		}
		catch (Exception error) {
			throw new IllegalStateException("内容读取失败：" + resource + "；" + error.getMessage(), error);
		}
	}

	/** 先校验结构再构造领域记录；错误保留实例字段路径，方便直接定位编辑错误。 */
	public static void validate(String resource, JsonNode document) {
		if (!resource.matches("/content/[a-z-]+\\.json"))
			throw new IllegalArgumentException("无效内容路径：" + resource);
		String name = resource.substring("/content/".length(), resource.length() - ".json".length());
		var schema = SCHEMAS.getSchema(SchemaLocation.of("classpath:content/schemas/" + name + ".schema.json"));
		var errors = schema.validate(document);
		if (!errors.isEmpty())
			throw new IllegalArgumentException("内容 Schema 校验失败：" + resource + "；"
					+ errors.stream().map(Object::toString).sorted().collect(Collectors.joining("；")));
	}

}
