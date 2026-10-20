package io.github.lishangbu.avalon.content.text;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.text.model.ForbiddenWordsDefinition;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 全部玩家自填文字共用的启动期违禁词目录。词条不按用途分类，规范化后采用包含匹配； 目录只回答是否命中，不改写原文、不返回命中词，也不决定费用、认证或事务。
 * 调用方只在新文字即将写入时检查，不能把它接入旧存档读取、密码或搜索条件。
 */
@Component
public class ForbiddenWordsCatalog {

	/** 只保存不可变的检测词条；启动后不重载文件，也不随某次请求改变匹配规则。 */
	private final List<String> terms;

	/** 与其他内容目录一起读取本地 JSON/Schema；缺文件或结构错误直接阻止启动。 */
	@Autowired
	public ForbiddenWordsCatalog() {
		this(ContentLoader.read("/content/rules/forbidden-words.json", ForbiddenWordsDefinition.class));
	}

	/**
	 * 构造时同时校验业务语义。空词表会关闭检测，纯分隔符规范化后会变成空串并命中所有文字， 两者均拒绝；大小写、全半角及分隔符等价的重复词也拒绝，便于维护者发现冗余配置。
	 * 此构造器供独立规则测试传入小词表，不提供运行时替换目录的入口。
	 */
	public ForbiddenWordsCatalog(ForbiddenWordsDefinition definition) {
		if (definition == null || definition.words() == null || definition.words().isEmpty())
			throw new IllegalArgumentException("/content/rules/forbidden-words.json：违禁词表不能为空");
		var unique = new HashSet<String>();
		var compiled = new ArrayList<String>();
		for (int index = 0; index < definition.words().size(); index++) {
			String word = definition.words().get(index);
			if (word == null || word.isBlank() || word.codePointCount(0, word.length()) > 80)
				throw invalid(index, "词条必须为1～80个码点的非空文字");
			String normalized = normalize(word);
			if (normalized.isEmpty())
				throw invalid(index, "词条规范化后为空");
			if (!unique.add(normalized))
				throw invalid(index, "词条规范化后重复");
			compiled.add(normalized);
		}
		terms = List.copyOf(compiled);
	}

	/**
	 * 使用统一规则检查原文的任意位置，英文不要求单词边界，因此短词也可能误拦。 空输入交给各字段原有必填校验；本方法不放宽原有长度或字符限制。
	 * 每次请求只规范化一次，词条在启动时已处理；当前小词表无需额外的正则或脚本执行器。
	 */
	public boolean contains(String text) {
		if (text == null || text.isEmpty())
			return false;
		String normalized = normalize(text);
		return terms.stream().anyMatch(normalized::contains);
	}

	/**
	 * NFKC统一兼容字符（包括全半角），再用Locale.ROOT忽略大小写；逐码点移除 空白、控制/格式字符、标点和符号，包括下划线、常见分隔符及插入的表情。
	 * 不做汉字简繁转换、拼音、谐音或相似字猜测，繁体需要在词表中显式登记。 返回值仅供检测，业务必须继续保存玩家原文，不能拿本结果作为名字或账号主键。
	 */
	private static String normalize(String text) {
		String compatible = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
		var result = new StringBuilder(compatible.length());
		compatible.codePoints().filter(codePoint -> !ignored(codePoint)).forEach(result::appendCodePoint);
		return result.toString();
	}

	/**
	 * 零宽格式符属于FORMAT；变体选择符及组合字素连接符虽归入组合标记，仍是 不可见分隔方式，需明确跳过。其他组合标记保留，不把任意带音调文字猜成英文词条。
	 */
	private static boolean ignored(int codePoint) {
		if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || codePoint == 0x034f
				|| (codePoint >= 0xfe00 && codePoint <= 0xfe0f) || (codePoint >= 0xe0100 && codePoint <= 0xe01ef))
			return true;
		return switch (Character.getType(codePoint)) {
			case Character.CONTROL, Character.FORMAT, Character.SPACE_SEPARATOR, Character.LINE_SEPARATOR,
					Character.PARAGRAPH_SEPARATOR, Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION,
					Character.START_PUNCTUATION, Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION,
					Character.FINAL_QUOTE_PUNCTUATION, Character.OTHER_PUNCTUATION, Character.MATH_SYMBOL,
					Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL, Character.OTHER_SYMBOL ->
				true;
			default -> false;
		};
	}

	/** 配置错误只标出文件和数组位置；业务拒绝消息由各命令统一提供。 */
	private static IllegalArgumentException invalid(int index, String reason) {
		return new IllegalArgumentException("/content/rules/forbidden-words.json words[" + index + "]：" + reason);
	}

}
