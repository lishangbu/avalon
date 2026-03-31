package io.github.lishangbu.avalon.game.service.config

import io.github.lishangbu.avalon.game.battle.engine.repository.EffectDefinitionRepository
import io.github.lishangbu.avalon.game.battle.engine.service.BattleSessionFactory
import io.github.lishangbu.avalon.game.battle.engine.service.BattleSessionStore
import io.github.lishangbu.avalon.game.service.effect.SmartBattleEffectImportService
import io.github.lishangbu.avalon.game.service.effect.SmartImportingEffectDefinitionRepository
import io.github.lishangbu.avalon.game.service.store.RedisBattleSessionStore
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.ObjectMapper

/**
 * `avalon-game-service` 默认装配。
 */
@Configuration(proxyBeanMethods = false)
class GameServiceConfiguration {
    @Bean
    @ConditionalOnMissingBean(EffectDefinitionRepository::class)
    fun effectDefinitionRepository(importService: SmartBattleEffectImportService): EffectDefinitionRepository = SmartImportingEffectDefinitionRepository(importService)

    @Bean
    @ConditionalOnBean(StringRedisTemplate::class)
    @ConditionalOnMissingBean
    fun redisBattleSessionStore(
        stringRedisTemplate: StringRedisTemplate,
        objectMapper: ObjectMapper,
        battleSessionFactory: BattleSessionFactory,
    ): BattleSessionStore =
        RedisBattleSessionStore(
            stringRedisTemplate = stringRedisTemplate,
            objectMapper = objectMapper,
            battleSessionFactory = battleSessionFactory,
        )
}
