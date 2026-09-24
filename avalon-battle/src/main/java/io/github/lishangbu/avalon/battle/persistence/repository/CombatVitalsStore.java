package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.CombatVitals;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/** 资源写入复用人物／伙伴主键，首次创建及后续更新加入调用者事务；查询直接使用类型化仓库。 */
@Repository
@RequiredArgsConstructor
public class CombatVitalsStore {

	private final CombatVitalsRepository vitals;

	public void saveVitals(long character, long id, int hp, int mp) {
		var value = vitals.get(id);
		if (value == null) {
			value = new CombatVitals();
			value.setId(id);
			value.setCharacterId(character);
			vitals.save(value);
		}
		value.setHp(hp);
		value.setMp(mp);
	}

}
