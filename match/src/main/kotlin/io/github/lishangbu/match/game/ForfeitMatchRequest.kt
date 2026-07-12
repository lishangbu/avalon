package io.github.lishangbu.match.game

/** 以共享 Match revision 防止基于过期 View 认输。 */
data class ForfeitMatchRequest(var expectedRevision: Long = -1)
