package com.guyteichman.mageknightbuddy.data

import com.guyteichman.mageknightbuddy.domain.Knight
import com.guyteichman.mageknightbuddy.domain.Outcome
import com.guyteichman.mageknightbuddy.domain.Scenario
import com.guyteichman.mageknightbuddy.domain.ScoringSession
import java.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Converts a domain [ScoringSession] to its Room row, encoding [ScoringSession.input] as JSON via [ScoringInputDto]. */
fun ScoringSession.toEntity(): ScoringSessionEntity = ScoringSessionEntity(
    scenario = scenario.id,
    knight = knight.name,
    playerName = playerName,
    inputJson = Json.encodeToString(input.toDto()),
    score = score,
    outcome = outcome.name,
    playedAtEpochMillis = playedAt.toEpochMilli(),
)

/** Converts a Room row back to a domain [ScoringSession], decoding [ScoringSessionEntity.inputJson] via [ScoringInputDto]. */
fun ScoringSessionEntity.toDomain(): ScoringSession = ScoringSession(
    scenario = Scenario.fromId(scenario),
    knight = Knight.valueOf(knight),
    playerName = playerName,
    input = Json.decodeFromString<ScoringInputDto>(inputJson).toDomain(),
    score = score,
    outcome = Outcome.valueOf(outcome),
    playedAt = Instant.ofEpochMilli(playedAtEpochMillis),
)
