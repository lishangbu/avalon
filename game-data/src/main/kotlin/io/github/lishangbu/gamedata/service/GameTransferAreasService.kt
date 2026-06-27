package io.github.lishangbu.gamedata.service

import io.github.lishangbu.gamedata.repository.GameTransferAreasRepository
import io.github.lishangbu.gamedata.dto.GameTransferAreasRequest
import io.github.lishangbu.gamedata.dto.GameTransferAreasResponse
import org.springframework.stereotype.Service

/**
 * 迁移区域 Service。
 */
@Service
class GameTransferAreasService(
	repository: GameTransferAreasRepository,
) : GameDataTableService<GameTransferAreasRequest, GameTransferAreasResponse>(
	repository,
	GameTransferAreasResponse::from,
)
