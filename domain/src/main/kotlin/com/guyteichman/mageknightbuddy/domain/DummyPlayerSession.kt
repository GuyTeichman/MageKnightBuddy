package com.guyteichman.mageknightbuddy.domain

import kotlin.random.Random

data class DummyPlayerSession private constructor(
    val knight: Knight,
    val wasRandom: Boolean,
    val deckOrder: List<CardColor>,
    val discardPile: List<CardColor>,
    val crystals: Map<CardColor, Int>,
    val round: Int,
    val roundEnded: Boolean,
    val log: List<DummyPlayerEvent>,
) {
    val remainingByColor: Map<CardColor, Int>
        get() = CardColor.entries.associateWith { color -> deckOrder.count { it == color } }

    fun playTurn(): DummyPlayerSession {
        if (roundEnded) return this

        if (deckOrder.isEmpty()) {
            return copy(roundEnded = true, log = log + DummyPlayerEvent.EndOfRoundAnnounced(round))
        }

        val initialReveal = deckOrder.take(3)
        val afterInitial = deckOrder.drop(3)
        val lastColor = initialReveal.last()
        val matchingCrystals = crystals.getValue(lastColor)
        val additionalCount = minOf(matchingCrystals, afterInitial.size)
        val additionalReveal = afterInitial.take(additionalCount)
        val remainingDeck = afterInitial.drop(additionalCount)

        return copy(
            deckOrder = remainingDeck,
            discardPile = discardPile + initialReveal + additionalReveal,
            log = log + DummyPlayerEvent.TurnPlayed(round, initialReveal, additionalReveal),
        )
    }

    fun endRound(advancedActionOfferColor: CardColor, spellOfferColor: CardColor): DummyPlayerSession = copy(
        deckOrder = (deckOrder + advancedActionOfferColor).shuffled(),
        crystals = crystals + (spellOfferColor to crystals.getValue(spellOfferColor) + 1),
        round = round + 1,
        roundEnded = false,
        log = log + DummyPlayerEvent.RoundEnded(round, advancedActionOfferColor, spellOfferColor),
    )

    companion object {
        private val STARTING_CRYSTAL_DOTS: Map<Knight, List<CardColor>> = mapOf(
            Knight.TOVAK to listOf(CardColor.RED, CardColor.BLUE, CardColor.BLUE),
            Knight.GOLDYX to listOf(CardColor.GREEN, CardColor.GREEN, CardColor.BLUE),
            Knight.NOROWAS to listOf(CardColor.GREEN, CardColor.WHITE, CardColor.WHITE),
            Knight.WOLFHAWK to listOf(CardColor.WHITE, CardColor.WHITE, CardColor.BLUE),
            Knight.ARYTHEA to listOf(CardColor.RED, CardColor.RED, CardColor.WHITE),
            Knight.KRANG to listOf(CardColor.RED, CardColor.RED, CardColor.GREEN),
            Knight.BRAEVALAR to listOf(CardColor.GREEN, CardColor.BLUE, CardColor.BLUE),
            Knight.CORAL to listOf(CardColor.BLUE, CardColor.BLUE, CardColor.RED),
        )

        private fun startingCrystals(knight: Knight): Map<CardColor, Int> {
            val dots = STARTING_CRYSTAL_DOTS.getValue(knight)
            return CardColor.entries.associateWith { color -> dots.count { it == color } }
        }

        fun start(
            knight: Knight,
            wasRandom: Boolean = false,
            deckOrder: List<CardColor> = CardColor.entries.flatMap { color -> List(4) { color } }.shuffled(),
        ): DummyPlayerSession = DummyPlayerSession(
            knight = knight,
            wasRandom = wasRandom,
            deckOrder = deckOrder,
            discardPile = emptyList(),
            crystals = startingCrystals(knight),
            round = 1,
            roundEnded = false,
            log = listOf(DummyPlayerEvent.RoundStarted(round = 1)),
        )

        fun startRandom(random: Random = Random): DummyPlayerSession {
            val knight = Knight.entries.toList().random(random)
            return start(knight, wasRandom = true)
        }
    }
}
