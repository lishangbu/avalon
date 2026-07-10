package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameGenders
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 性别资料持久化访问。
 */
interface GameGendersRepository : KRepository<GameGenders, Long>
