package io.github.lishangbu.security.repository

import io.github.lishangbu.security.entity.SecurityTokenState
import org.babyfish.jimmer.spring.repository.KRepository

/** Registers Sa-Token state with Jimmer's managed entity set; mutations remain encapsulated by the DAO. */
interface SecurityTokenStateRepository : KRepository<SecurityTokenState, Long>
