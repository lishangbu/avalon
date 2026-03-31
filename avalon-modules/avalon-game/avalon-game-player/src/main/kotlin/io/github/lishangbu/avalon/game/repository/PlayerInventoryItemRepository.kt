package io.github.lishangbu.avalon.game.repository

import io.github.lishangbu.avalon.game.entity.PlayerInventoryItem
import org.babyfish.jimmer.spring.repository.KRepository

interface PlayerInventoryItemRepository : KRepository<PlayerInventoryItem, Long>
