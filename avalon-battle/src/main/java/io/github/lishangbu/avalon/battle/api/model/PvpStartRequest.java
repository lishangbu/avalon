package io.github.lishangbu.avalon.battle.api.model;

import java.util.UUID;

public record PvpStartRequest(Long opponentCharacter, UUID requestId) {
}
