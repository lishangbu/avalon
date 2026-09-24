package io.github.lishangbu.avalon.content.creature;

import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;

/** 改名收费与长度在启动时加载；界面预览和写命令共用同一配置。 */
@Component
public class CreatureNamingCatalog {

	private final Definition definition = ContentLoader.read("/content/creature-naming.json", Definition.class);

	public CreatureNamingCatalog() {
		if (definition.firstPrice() < 0 || definition.repeatPrice() < 0 || definition.maxLength() < 1
				|| definition.maxLength() > 40)
			throw new IllegalArgumentException("伙伴改名配置无效");
	}

	public Definition definition() {
		return definition;
	}

	public int price(int renamed) {
		return renamed == 0 ? definition.firstPrice() : definition.repeatPrice();
	}

	public record Definition(int firstPrice, int repeatPrice, int maxLength) {
	}

}
