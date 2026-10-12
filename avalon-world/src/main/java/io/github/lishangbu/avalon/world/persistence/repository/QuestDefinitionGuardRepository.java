package io.github.lishangbu.avalon.world.persistence.repository;

import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import io.github.lishangbu.avalon.world.persistence.entity.QuestDefinitionGuard;

/** 保存最近一次部署的结构基线；不提供按版本查询，也不用于运行时取定义。 */
public interface QuestDefinitionGuardRepository extends EntityRepository<QuestDefinitionGuard, String> {

}
