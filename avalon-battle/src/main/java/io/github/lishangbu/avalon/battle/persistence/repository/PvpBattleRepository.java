package io.github.lishangbu.avalon.battle.persistence.repository;

import io.github.lishangbu.avalon.battle.persistence.entity.PvpBattle;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import org.springframework.data.jpa.repository.Query;

public interface PvpBattleRepository extends EntityRepository<PvpBattle, Long> {

	PvpBattle findFirstByAttackerIdOrDefenderIdOrderByIdDesc(long attacker, long defender);

	@Query("from PvpBattle where attackerId = ?1 and requestId = ?2")
	PvpBattle request(long attacker, String request);

	@Query("select new io.github.lishangbu.avalon.battle.application.model.PvpParticipants(attackerId, defenderId) from PvpBattle where id = ?1")
	io.github.lishangbu.avalon.battle.application.model.PvpParticipants participants(long id);

}
