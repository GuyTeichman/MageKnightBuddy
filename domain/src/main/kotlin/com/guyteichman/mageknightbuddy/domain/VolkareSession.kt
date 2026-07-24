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
    // Whether this session began on a night Round (Round 1 = night) instead of the usual day
    // start - set once at setup, never changed afterward. See [isDay].
    val startsAtNight: Boolean = false,
) {
    /**
     * Whether the current [round] is a day round - see [isDayRound] for the odd/even derivation
     * from [startsAtNight]. No rule of Volkare's currently reads this; it's tracked for parity
     * with [DummyPlayerSession]/[ProxyPlayerSession] ahead of a planned day/night indicator.
     */
    val isDay: Boolean
        get() = isDayRound(round, startsAtNight)

    /**
     * How many turns have been played so far in the current [round] - mirrors
     * [DummyPlayerSession.turnInRound] (issue #125). Counts both [VolkareEvent.CardRevealed] (a
     * normal reveal) and [VolkareEvent.Frenzy] (Volkare's Return's repeatable empty-deck turn) as
     * played turns, since both are logged by an actual [playTurn] call; [VolkareEvent.QuestLost]
     * is a marker logged *alongside* the [CardRevealed] for the same turn, not a turn of its own,
     * so it's excluded to avoid double-counting that turn. Filtered by the event's own `round`
     * field, not log position since the last round-boundary entry, for the same reason
     * [DummyPlayerSession.turnInRound] is: `endRound()` never logs a fresh `RoundStarted`.
     */
    val turnInRound: Int
        get() = log.count { event ->
            when (event) {
                is VolkareEvent.CardRevealed -> event.round == round
                is VolkareEvent.Frenzy -> event.round == round
                else -> false
            }
        }

    /**
     * Plays one Volkare turn: reveals the top card of [deckOrder] onto [discardPile] and logs it.
     * If the deck is already empty (a defensive fallback - see below for the real trigger), the
     * fork is by scenario: Volkare's Return logs a [Frenzy] event and stays playable forever (his
     * deck never reshuffles - see `CONTEXT.md`'s "Frenzy" entry); Volkare's Quest sets [lost] to
     * true and logs [QuestLost]. Mirrors [DummyPlayerSession.playTurn]'s early-return guard
     * pattern: once [lost] is true, further calls are a no-op, since [lost] never becomes true in
     * Volkare's Return, this guard only ever actually blocks Volkare's Quest.
     *
     * In Volkare's Quest, though, the real losing moment is earlier than an empty deck: revealing
     * the *last card that could still move him toward the portal* is already his final move into
     * it. That's every green/blue/white [VolkareCard.BasicAction]/[VolkareCard.CompetitiveSpell] -
     * Wounds never move him, and red cards never move him in Quest either (he only ever attacks on
     * red - see docs/rules/volkares-quest.md's "Course of the Game", "this rule holds for the
     * entire scenario"), so neither counts toward "should already be at the portal by now" (see
     * `CONTEXT.md`'s "Frenzy" entry). [lost] is set the instant that last green/blue/white card is
     * revealed, whether or not Wounds or red cards still trail it in the deck - those are simply
     * never drawn, since [playTurn] no-ops once [lost] is true. A red card can still be revealed
     * and narrated normally (he attacks) without ending the scenario, as long as a green/blue/white
     * card remains somewhere after it.
     *
     * [manaRoll] is the mana die result to use *if* the revealed card turns out to be a
     * [VolkareCard.Wound] (only Wound reveals ever roll the die - see `CONTEXT.md`'s "Wound"
     * handling). Defaults to a fresh random roll, same spirit as [start]'s `deckOrder` defaulting
     * to a fresh shuffle - passing it explicitly (as tests do) skips the randomness for
     * deterministic assertions. Ignored (never logged) when the revealed card isn't a Wound.
     */
    fun playTurn(manaRoll: ManaColor = ManaColor.entries.random()): VolkareSession {
        if (lost) return this

        if (deckOrder.isEmpty()) {
            return when (scenario) {
                Scenario.VolkaresReturn -> copy(log = log + VolkareEvent.Frenzy(round))
                Scenario.VolkaresQuest -> copy(lost = true, log = log + VolkareEvent.QuestLost(round))
                else -> throw IllegalStateException("VolkareSession only supports Volkare's Return/Quest, got $scenario")
            }
        }

        val card = deckOrder.first()
        val remainingDeck = deckOrder.drop(1)
        val cardRevealed = VolkareEvent.CardRevealed(
            round = round,
            card = card,
            cityRevealed = cityRevealed,
            manaRoll = if (card is VolkareCard.Wound) manaRoll else null,
        )

        val isFinalMoveInQuest = scenario == Scenario.VolkaresQuest &&
            card.movesTowardPortalInQuest &&
            remainingDeck.none { it.movesTowardPortalInQuest }

        return if (isFinalMoveInQuest) {
            copy(deckOrder = remainingDeck, discardPile = discardPile + card, lost = true, log = log + cardRevealed + VolkareEvent.QuestLost(round))
        } else {
            copy(deckOrder = remainingDeck, discardPile = discardPile + card, log = log + cardRevealed)
        }
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
            startsAtNight: Boolean = false,
        ): VolkareSession = VolkareSession(
            scenario = scenario,
            raceLevel = raceLevel,
            deckOrder = deckOrder,
            discardPile = emptyList(),
            round = 1,
            cityRevealed = false,
            lost = false,
            log = listOf(VolkareEvent.RoundStarted(round = 1)),
            startsAtNight = startsAtNight,
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
            startsAtNight: Boolean = false,
        ): VolkareSession = VolkareSession(
            scenario = scenario,
            raceLevel = raceLevel,
            deckOrder = deckOrder,
            discardPile = discardPile,
            round = round,
            cityRevealed = cityRevealed,
            lost = lost,
            log = log,
            startsAtNight = startsAtNight,
        )
    }
}

/**
 * Whether revealing this card, in Volkare's Quest, ever moves him toward the portal - only
 * green/blue/white [VolkareCard.BasicAction]/[VolkareCard.CompetitiveSpell] cards do (see
 * docs/rules/volkares-quest.md's "Course of the Game": red cards only ever trigger an attack in
 * Quest, "this rule holds for the entire scenario", and Wounds never move him at all). Used by
 * [VolkareSession.playTurn] to find the *last* card that could still move him toward the portal -
 * see that function's doc comment.
 */
private val VolkareCard.movesTowardPortalInQuest: Boolean
    get() = when (this) {
        is VolkareCard.BasicAction -> color != CardColor.RED
        is VolkareCard.CompetitiveSpell -> color != CardColor.RED
        VolkareCard.Wound -> false
    }
