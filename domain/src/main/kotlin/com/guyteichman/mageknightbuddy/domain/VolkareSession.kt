package com.guyteichman.mageknightbuddy.domain

/**
 * Immutable snapshot of Volkare's state at a point in time - his deck, discard pile, current
 * round, and a log of events so far - for the Dummy Player tab's Volkare mode (Volkare's Return /
 * Volkare's Quest, The Lost Legion expansion). Mirrors [DummyPlayerSession]'s shape (private
 * constructor + companion-object factory functions), but is deliberately narrower in what it
 * tracks: it reveals cards from Volkare's deck for the player to interpret by hand against the
 * physical rulebook's movement/combat procedure, rather than simulating Volkare's board position
 * or combat outcomes - see ADR-0004 and `CONTEXT.md`'s "Volkare Session" entry.
 *
 * As with [DummyPlayerSession], every method here (`playTurn`, `endRound`, `toggleCityRevealed`)
 * returns a **new** `VolkareSession` rather than mutating this one, and the constructor is
 * `private` - the only way to obtain an instance is through [start] (begin a new session) or
 * [restore] (reconstruct one from previously-persisted state).
 */
data class VolkareSession private constructor(
    val scenario: Scenario,
    val raceLevel: RaceLevel,
    val deckOrder: List<VolkareCard>,
    val discardPile: List<VolkareCard>,
    val round: Int,
    // Return-only in practice: always false and never read for Volkare's Quest, which has no
    // equivalent city-reveal step - see CONTEXT.md's "City Revealed" entry.
    val cityRevealed: Boolean,
    // Quest-only in practice: Volkare's Return never sets this - it enters Frenzy instead.
    val lost: Boolean,
    val log: List<VolkareEvent>,
) {
    /**
     * Plays one Volkare turn: reveals the top card of [deckOrder] onto [discardPile] and logs it.
     * If the deck is already empty, behavior forks by scenario - Volkare's Return logs a [Frenzy]
     * event and stays playable forever (his deck never reshuffles - see `CONTEXT.md`'s "Frenzy"
     * entry); Volkare's Quest instead sets [lost] to true and logs [QuestLost]. Mirrors
     * [DummyPlayerSession.playTurn]'s early-return guard pattern: once [lost] is true, further
     * calls are a no-op, since [lost] never becomes true in Volkare's Return, this guard only
     * ever actually blocks Volkare's Quest.
     */
    fun playTurn(): VolkareSession {
        if (lost) return this

        if (deckOrder.isEmpty()) {
            return when (scenario) {
                Scenario.VolkaresReturn -> copy(log = log + VolkareEvent.Frenzy(round))
                Scenario.VolkaresQuest -> copy(lost = true, log = log + VolkareEvent.QuestLost(round))
                else -> throw IllegalStateException("VolkareSession only supports Volkare's Return/Quest, got $scenario")
            }
        }

        val card = deckOrder.first()
        return copy(
            deckOrder = deckOrder.drop(1),
            discardPile = discardPile + card,
            log = log + VolkareEvent.CardRevealed(round, card, cityRevealed),
        )
    }

    /**
     * Advances to the next round. Unlike [DummyPlayerSession.endRound], this has no reshuffle or
     * crystal/offer interaction to apply - for Volkare, "ending round is purely a player
     * convenience for tracking, not a game mechanic" (issue #128), so this just increments
     * [round] and logs it.
     */
    fun endRound(): VolkareSession = copy(round = round + 1, log = log + VolkareEvent.RoundEnded(round))

    /**
     * Flips the [cityRevealed] flag - see `CONTEXT.md`'s "City Revealed" entry. Meaningful for
     * Volkare's Return only; the UI only exposes this toggle when [scenario] is
     * [Scenario.VolkaresReturn], but the flag itself is harmless to flip for Volkare's Quest since
     * nothing there ever reads it.
     */
    fun toggleCityRevealed(): VolkareSession = copy(cityRevealed = !cityRevealed)

    companion object {
        /**
         * Builds Volkare's starting deck (docs/rules/volkares-return.md / volkares-quest.md's
         * "Setup"): the 16 Basic Action cards of an unused Hero (4 per [CardColor]), the 4
         * removed competitive Spells (1 per [CardColor]), and [woundCount] Wounds - shuffled
         * together by the caller (see [start]'s default `deckOrder`).
         */
        private fun buildDeck(woundCount: Int): List<VolkareCard> =
            // flatMap expands each color into 4 Basic Actions of that color, then flattens all
            // four colors' lists into one combined list of 16 cards - same idiom as
            // DummyPlayerSession.start's default deckOrder.
            CardColor.entries.flatMap { color -> List(4) { VolkareCard.BasicAction(color) } } +
                CardColor.entries.map { color -> VolkareCard.CompetitiveSpell(color) } +
                List(woundCount) { VolkareCard.Wound }

        /**
         * Begins a new Volkare session for the given [scenario] (must be
         * [Scenario.VolkaresReturn] or [Scenario.VolkaresQuest]) and [raceLevel]. [woundCount]
         * defaults to that scenario/level's table value ([volkareWoundCount]) but can be
         * overridden (the setup screen's directly-editable Wound-count field). [deckOrder]
         * defaults to [buildDeck]'s cards shuffled; passing it explicitly (as tests do) skips the
         * shuffle for deterministic assertions.
         */
        fun start(
            scenario: Scenario,
            raceLevel: RaceLevel,
            woundCount: Int = volkareWoundCount(scenario, raceLevel),
            deckOrder: List<VolkareCard> = buildDeck(woundCount).shuffled(),
        ): VolkareSession = VolkareSession(
            scenario = scenario,
            raceLevel = raceLevel,
            deckOrder = deckOrder,
            discardPile = emptyList(),
            round = 1,
            cityRevealed = false,
            lost = false,
            log = listOf(VolkareEvent.RoundStarted(round = 1)),
        )

        /**
         * Reconstructs a session from its full persisted state (used by the persistence layer to
         * restore a saved game). Not for general use - [start] is the entry point for beginning a
         * new session.
         */
        fun restore(
            scenario: Scenario,
            raceLevel: RaceLevel,
            deckOrder: List<VolkareCard>,
            discardPile: List<VolkareCard>,
            round: Int,
            cityRevealed: Boolean,
            lost: Boolean,
            log: List<VolkareEvent>,
        ): VolkareSession = VolkareSession(
            scenario = scenario,
            raceLevel = raceLevel,
            deckOrder = deckOrder,
            discardPile = discardPile,
            round = round,
            cityRevealed = cityRevealed,
            lost = lost,
            log = log,
        )
    }
}
