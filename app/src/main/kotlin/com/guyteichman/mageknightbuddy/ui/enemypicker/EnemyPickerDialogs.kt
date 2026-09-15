package com.guyteichman.mageknightbuddy.ui.enemypicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.DrawLogEntry
import com.guyteichman.mageknightbuddy.domain.EnemyToken
import com.guyteichman.mageknightbuddy.domain.FactionRewardToken
import com.guyteichman.mageknightbuddy.domain.FactionRewardTokenCatalogue
import com.guyteichman.mageknightbuddy.domain.PossessedEnemy
import com.guyteichman.mageknightbuddy.domain.PossessedTokenCatalogue
import com.guyteichman.mageknightbuddy.domain.RuinToken
import com.guyteichman.mageknightbuddy.domain.RuinTokenCatalogue
import com.guyteichman.mageknightbuddy.domain.TokenCatalogue
import com.guyteichman.mageknightbuddy.domain.TokenPile
import com.guyteichman.mageknightbuddy.domain.TokenPileId
import com.guyteichman.mageknightbuddy.ui.components.LabeledSwitch

/**
 * The stateless, parameter-driven detail/overview dialogs for the Enemy Picker (issue #317 split):
 * everything [EnemyPickerScreen.kt][EnemyPickerTab] pops open on top of the main content - zoomed
 * token/ruin/reward views, the multi-token grid overviews, the "?" ability window, the pile-contents
 * survey and the Defeat dialog. Split out purely to keep `EnemyPickerScreen.kt` a manageable size;
 * these dialogs still read the screen-local state types ([ZoomState]/[GridState]) and helpers
 * (`internal` in that file) since both files share this package.
 */

/**
 * The centered Close + primary-action button pair shown at the bottom of a token/ruin/reward zoom
 * dialog (issue #227): Close is always available, and the action button does double duty as "mark
 * done and dismiss" (disabled once already done, since there's nothing left to do). Shared by
 * [TokenZoomDialog]/[RuinZoomDialog]/[FactionRewardZoomDialog], which differ only in their button's
 * label pair ("Defeat"/"Defeated", "Resolve"/"Resolved", "Spend"/"Spent") and what [onDefeat] flips.
 * Built as a plain centered Row (not AlertDialog's confirmButton/dismissButton slots) because
 * Material3 always end-aligns that row, with no way to center it.
 */
@Composable
internal fun ZoomActionFooter(
    defeated: Boolean,
    activeLabel: String,
    doneLabel: String,
    onDefeat: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onDismiss) { Text("Close") }
        // Doing the flip and closing together (issue #227) - you almost always want a just-resolved
        // dialog gone immediately, so folding the Close tap into the action button spares a tap.
        Button(
            onClick = { onDefeat(); onDismiss() },
            enabled = !defeated,
        ) {
            Text(if (defeated) doneLabel else activeLabel)
        }
    }
}

/**
 * The "< x of y >" prev/next row shown when a zoom dialog is viewing one entry out of a multi-token
 * batch (D3/D7) - identical in [TokenZoomDialog] and [SummonedChildZoomDialog], so it's shared here.
 * Renders nothing for a single-entry batch ([ZoomState.logIndices] of size 1), matching the inlined
 * `if (state.logIndices.size > 1)` guard both dialogs used to repeat before this was factored out.
 */
@Composable
internal fun ZoomNavRow(state: ZoomState, onNavigate: (Int) -> Unit) {
    if (state.logIndices.size > 1) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onNavigate(state.index - 1) }, enabled = state.index > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
            }
            Text("${state.index + 1} of ${state.logIndices.size}")
            IconButton(onClick = { onNavigate(state.index + 1) }, enabled = state.index < state.logIndices.size - 1) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
            }
        }
    }
}

/**
 * Zoomed view of a drawn token (or a tapped log entry): its art (or text fallback), name, stat line
 * and attacks, a "?" to open the full ability info window, prev/next with an "x of y" counter when a
 * whole batch was drawn at once, a Summon/Re-summon button when the token has a Summon attack
 * (issue #191), and a Defeat button (D2/D11) that also closes the window (issue #227).
 *
 * [log] is the whole Draw Log so [state]'s indices can be resolved to entries; that lookup also
 * drives the Defeat button's own state (whether *this* entry is already defeated), since the same
 * dialog instance stays open across [onNavigate] calls as the user flips through a batch.
 * [currentChildrenOf] resolves a Summon Draw's current children (see `EnemyPickerSession`), used
 * both to label the button ("Summon" vs "Re-summon") and to list what's currently summoned - tapping
 * that list ([onViewSummoned]) re-opens the existing child(ren) without drawing again, distinct from
 * [onSummon] which always draws a fresh set.
 */
@Composable
internal fun TokenZoomDialog(
    state: ZoomState,
    log: List<DrawLogEntry>,
    onNavigate: (Int) -> Unit,
    onShowInfo: (EnemyToken) -> Unit,
    onToggleDefeated: (Int, Boolean) -> Unit,
    onSummon: (Int) -> Unit,
    onViewSummoned: (List<Int>) -> Unit,
    currentChildrenOf: (Int) -> List<Int>,
    onDismiss: () -> Unit,
) {
    val logIndex = state.logIndices[state.index]
    val entry = log[logIndex]
    val token = TokenCatalogue.byId(entry.tokenId)
    val summonPiles = token?.attacks?.filter { it.isSummon } ?: emptyList()
    val currentChildren = if (summonPiles.isEmpty()) emptyList() else currentChildrenOf(logIndex)
    AlertDialog(
        onDismissRequest = onDismiss,
        // Material3's AlertDialog always end-aligns its confirmButton/dismissButton row, with no
        // way to center it - so Close/Defeat are built as an ordinary centered Row inside `text`
        // instead (below), and this slot is left empty to satisfy the required parameter.
        confirmButton = {},
        title = {
            // Box (not a Row) so the name can be centered across the full width via its own
            // fillMaxWidth + TextAlign.Center, with the "?" button floated at the end on top of
            // it - a Row would instead push the name off-center to make room for the button.
            Box(Modifier.fillMaxWidth()) {
                Text(
                    text = token?.name ?: entry.tokenId,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                if (token != null) {
                    IconButton(onClick = { onShowInfo(token) }, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Filled.QuestionMark, contentDescription = "Abilities")
                    }
                }
            }
        },
        text = {
            // fillMaxWidth so horizontalAlignment actually centers each line across the dialog's
            // full content width - without it the Column shrink-wraps to its widest child and
            // shorter lines (stat line, attacks) end up flush-left instead of centered.
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // A possessed enemy (Apocalypse Dragon): resolve the possessed token so the face
                // superimposes the circular enemy on it and the stats below are the *summed* values
                // (never the deltas). Null for an ordinary draw. See docs/rules/apocalypse-dragon.md.
                val possessed = entry.possessedToken()
                val stats = if (token != null && possessed != null) PossessedEnemy.combine(token, possessed) else null
                if (token != null) {
                    if (possessed != null) {
                        PossessedEnemyFace(circular = token, possessed = possessed, size = 140.dp)
                        Text(
                            "Possessed enemy",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        EnemyTokenFaceWithSummon(
                            token = token,
                            size = 140.dp,
                            // Left side, not the default corner (D-issue #191 follow-up note): that's
                            // where a printed Summon attack shows its pile-color icon, so the current
                            // child's own face standing in that exact spot reads as "this is who's
                            // actually fighting" rather than a decorative badge. All current children are
                            // passed (not just the first) so a double-summoner shows both over its two slots.
                            summonedChildren = currentChildren.mapNotNull { TokenCatalogue.byId(log[it].tokenId) },
                            alignment = Alignment.CenterStart,
                        )
                    }
                    // Stat line and attacks show the summed values when possessed, the token's own otherwise.
                    Text(stats?.statLine() ?: token.statLine(), style = MaterialTheme.typography.titleMedium)
                    (stats?.attacks ?: token.attacks).forEach { attack ->
                        if (attack.isSummon) {
                            // A summon has no attack value - it draws a replacement from another pile.
                            Text("Summons a ${attack.summons?.summonName() ?: "token"}")
                        } else {
                            Text("Attack ${attack.value} · ${attack.element.displayName()}")
                        }
                    }
                    // The possessed token's added Psychic Attack (elementless; ignores offensive abilities).
                    stats?.psychicAttack?.let { psychic ->
                        Text(
                            "Psychic Attack $psychic",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    // Offensive abilities (Brutal, Swift, ...) are whole-token, so they're shown once
                    // beneath the attack(s) rather than tacked onto each. Full text is in the "?" window.
                    if (token.offensiveAbilities.isNotEmpty()) {
                        Text(
                            token.offensiveAbilities.joinToString { it.describe().first },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Every possessed enemy rewards a Faction token when defeated - a faction-agnostic
                    // reminder here (the faction is named by the scenario and tracked by the separate
                    // faction-token feature, not the picker).
                    if (possessed != null) {
                        Text(
                            "Reward: a Faction token",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (summonPiles.isNotEmpty()) {
                    if (currentChildren.isNotEmpty()) {
                        val names = currentChildren.joinToString { TokenCatalogue.byId(log[it].tokenId)?.name ?: log[it].tokenId }
                        // Tapping re-opens the existing child(ren) (their own zoom, read-only) without
                        // drawing again - underlined + primary-colored so it reads as tappable, distinct
                        // from the Summon/Re-summon button below which always draws a fresh set.
                        Text(
                            "Summoned: $names",
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onViewSummoned(currentChildren) },
                        )
                    }
                    Button(onClick = { onSummon(logIndex) }, enabled = !entry.defeated) {
                        Text(if (currentChildren.isEmpty()) "Summon" else "Re-summon")
                    }
                }
                ZoomNavRow(state = state, onNavigate = onNavigate)
                // D11: Defeat is the primary (filled) action; Close stays a plain text button beside
                // it, in that left-to-right order (see [ZoomActionFooter]'s own doc for why this is a
                // custom Row, not AlertDialog's button slots). Defeating also closes the window
                // (issue #227) - onDismiss here falls back to the grid this zoom was drilled into
                // from, if any (see onDismiss's own comment at the call site), so the next batch
                // member is still one tap away.
                ZoomActionFooter(
                    defeated = entry.defeated,
                    activeLabel = "Defeat",
                    doneLabel = "Defeated",
                    onDefeat = { onToggleDefeated(logIndex, true) },
                    onDismiss = onDismiss,
                )
            }
        },
    )
}

/**
 * A Summon Draw child's own zoomed view (issue #191): art, attacks and offensive abilities,
 * Close-only - narrower than [TokenZoomDialog] in two ways. First, no Armor/Fame/resistances/
 * defensive abilities: a child is narrated by what it *does* to you (attacks, offensive abilities),
 * not what it withstands - that fuller reference is still one "?" tap away via [TokenInfoDialog].
 * Second, no Defeat or Summon actions - a child is never independently marked Defeated (the
 * summoner's own Defeat flag resolves the whole encounter) and never itself offers a Summon action -
 * so this is a deliberately simpler sibling rather than [TokenZoomDialog] with flags threaded
 * through it for a case that only ever reads, never acts.
 */
@Composable
internal fun SummonedChildZoomDialog(
    state: ZoomState,
    log: List<DrawLogEntry>,
    onNavigate: (Int) -> Unit,
    onShowInfo: (EnemyToken) -> Unit,
    onDismiss: () -> Unit,
) {
    val entry = log[state.logIndices[state.index]]
    val token = TokenCatalogue.byId(entry.tokenId)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Box(Modifier.fillMaxWidth()) {
                // "(Summoned)" suffix - unlike TokenZoomDialog's title, this dialog is always a
                // Summon Draw child, so the title itself says so rather than requiring the "Summon"
                // text on the parent's own dialog to be remembered. End padding reserves room for
                // the "?" button below so this longer title doesn't run into it before centering.
                Text(
                    text = "${token?.name ?: entry.tokenId} (Summoned)",
                    modifier = Modifier.fillMaxWidth().padding(end = 40.dp),
                    textAlign = TextAlign.Center,
                )
                if (token != null) {
                    IconButton(onClick = { onShowInfo(token) }, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Filled.QuestionMark, contentDescription = "Abilities")
                    }
                }
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (token != null) {
                    EnemyTokenFace(token = token, size = 140.dp)
                    // Deliberately no statLine() here (Armor/Fame) and no resistances/defensive
                    // abilities, unlike the summoner's own zoom - a child is narrated by what it
                    // *does* (its attacks, its offensive abilities), not what it withstands; the
                    // full reference (including Armor/Fame/resistances) is still one "?" tap away
                    // via TokenInfoDialog for whoever wants it.
                    // If a *possessed* summoner drew this child, the possessed Attack delta applies to
                    // this token's topmost attack instead of the summoner's (rulebook p.7), so the
                    // number shown is the summed one.
                    // Only the summed number is shown, never the delta (the "no deltas alongside
                    // sums" rule) - the possessed summoner's boost is already baked into the value.
                    val summonDelta = summonDeltaFor(state.logIndices[state.index], log)
                    PossessedEnemy.withTopmostAttackDelta(token.attacks, summonDelta).forEach { attack ->
                        if (attack.isSummon) {
                            Text("Summons a ${attack.summons?.summonName() ?: "token"}")
                        } else {
                            Text("Attack ${attack.value} · ${attack.element.displayName()}")
                        }
                    }
                    if (token.offensiveAbilities.isNotEmpty()) {
                        Text(
                            token.offensiveAbilities.joinToString { it.describe().first },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ZoomNavRow(state = state, onNavigate = onNavigate)
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

/**
 * The read-only body shared by [RuinZoomDialog] and [RuinInfoDialog] (issue #231): the ruin's face,
 * then either the Ancient Altar's mana prompt + derived Fame, or the Enemies-With-Treasure draw
 * prompt + its printed (reference-only) reward. [extraContent] renders right after that reward line -
 * [RuinZoomDialog] uses it for the "Draw its enemies" button / "Enemies: …" line, which
 * [RuinInfoDialog] has no counterpart for (it's a read-only pile-survey preview, not a drawn ruin), so
 * it defaults to nothing.
 */
@Composable
internal fun RuinBody(ruin: RuinToken, extraContent: @Composable () -> Unit = {}) {
    RuinTokenFace(ruin = ruin, size = 140.dp)
    if (ruin.isAltar) {
        Text(ruin.altarPrompt(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text("Gain ${ruin.altarFame()} Fame", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Text(ruin.enemyDrawPrompt(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        // Reward is reference text only (ADR-0006 amended to display, not track it).
        ruin.reward?.let { Text("Reward: $it", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
        extraContent()
    }
}

/**
 * Zoomed view of a drawn Ruin token (issue #201) - the RUIN pile's counterpart to [TokenZoomDialog],
 * since a ruin prints no armor/attack/fame block. An **Ancient Altar** shows its mana-payment prompt
 * and the Fame it grants (derived, never scored - ADR-0006); an **Enemies With Treasure** shows its
 * draw instruction, its printed [RuinToken.reward] (flavour/reference only), and a one-shot "Draw its
 * enemies" button that draws the prescribed enemies via [onDrawEnemies] and attaches them under this
 * ruin. Once drawn, that button is replaced by a tappable "Enemies: …" line ([onViewEnemies]) - a
 * ruin group is drawn once and stays put (no re-draw). Both kinds carry the same Defeat + Close pair
 * as the enemy zoom, marking the whole ruin resolved.
 */
@Composable
internal fun RuinZoomDialog(
    ruin: RuinToken,
    entry: DrawLogEntry,
    logIndex: Int,
    currentEnemies: List<Int>,
    enemyName: (Int) -> String,
    onDrawEnemies: (Int) -> Unit,
    onViewEnemies: (List<Int>) -> Unit,
    onToggleDefeated: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = if (ruin.isAltar) "Ancient Altar" else "Enemies with Treasure",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RuinBody(ruin = ruin) {
                    if (currentEnemies.isEmpty()) {
                        Button(onClick = { onDrawEnemies(logIndex) }, enabled = !entry.defeated) {
                            Text("Draw its enemies")
                        }
                    } else {
                        // A ruin group is one-shot: once its enemies are out, offer to re-open them
                        // (full stats, per-enemy Defeat) rather than draw a fresh set.
                        Text(
                            "Enemies: " + currentEnemies.joinToString { enemyName(it) },
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clickable { onViewEnemies(currentEnemies) },
                        )
                    }
                }

                // Defeating a ruin (paid the altar / cleared its enemies) closes the window too,
                // matching the enemy zoom (issue #227).
                ZoomActionFooter(
                    defeated = entry.defeated,
                    activeLabel = "Resolve",
                    doneLabel = "Resolved",
                    onDefeat = { onToggleDefeated(logIndex, true) },
                    onDismiss = onDismiss,
                )
            }
        },
    )
}

/**
 * The read-only body shared by [FactionRewardZoomDialog] and [RewardInfoDialog] (issue #252): the
 * token's square face, its effect text, and the discard-for-Fame/Influence footer every reward token
 * prints. [FactionRewardZoomDialog] appends its own Spend footer after this; [RewardInfoDialog] (a
 * read-only pile-survey preview) does not.
 */
@Composable
internal fun RewardBody(token: FactionRewardToken) {
    FactionRewardTokenFace(token = token, size = 140.dp)
    Text(token.effectText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    // The line printed on every reward token, held once here rather than in each token's
    // effectText (see FactionRewardToken's doc) - shown so the player knows the option.
    Text(
        FACTION_REWARD_DISCARD_FOOTER,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * Zoomed view of a drawn (or revisited) faction reward token (issue #252) - the reward piles'
 * counterpart to [TokenZoomDialog]/[RuinZoomDialog], since a reward token prints no armor/attack/fame
 * and no altar/enemy-draw prompt, only a one-off effect. Shows its square tile face, its effect text
 * (reference only, never resolved - ADR-0006), the universal discard-for-Fame/Influence footer, and a
 * **Spend** action - the reward's "Defeat", flipping the same [DrawLogEntry.defeated] flag so a spent
 * token drops out of the held pinned entry into the dimmed history (interim behaviour; issue #251
 * makes that flag pile-correct). Spending also closes the window, matching the enemy/ruin zooms.
 */
@Composable
internal fun FactionRewardZoomDialog(
    token: FactionRewardToken,
    entry: DrawLogEntry,
    logIndex: Int,
    onToggleSpent: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(token.name, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RewardBody(token = token)
                ZoomActionFooter(
                    defeated = entry.defeated,
                    activeLabel = "Spend",
                    doneLabel = "Spent",
                    onDefeat = { onToggleSpent(logIndex, true) },
                    onDismiss = onDismiss,
                )
            }
        },
    )
}

/**
 * The adaptive [LazyVerticalGrid] shell shared by [TokenGridDialog], [HeldRewardsDialog] and
 * [PileContentsGrid] (D9): columns that pack as many [GRID_CELL_MIN_SIZE]-or-wider cells per row as
 * fit, capped at [GRID_MAX_HEIGHT] tall so a rare large batch scrolls internally instead of growing
 * the dialog (D10). [content] is the grid's own `items`/`itemsIndexed` call - callers differ only in
 * what they iterate and how they key it.
 */
@Composable
internal fun TokenGrid(modifier: Modifier = Modifier, content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GRID_CELL_MIN_SIZE),
        modifier = modifier.heightIn(max = GRID_MAX_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * The held-faction-rewards grid (issue #252): every currently-held reward token, reached from the
 * pinned Draw Log entry. One cell per held token (face + name + a **Spend** checkbox); tapping the
 * cell opens that token's [FactionRewardZoomDialog] detail. Spending a token flips its
 * [DrawLogEntry.defeated] flag, which drops it out of [heldIndices] on the next recomposition, so it
 * leaves this grid and joins the dimmed history. Reuses the same adaptive grid as [TokenGridDialog].
 */
@Composable
internal fun HeldRewardsDialog(
    heldIndices: List<Int>,
    log: List<DrawLogEntry>,
    onOpenDetail: (Int) -> Unit,
    onSpend: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Faction rewards held") },
        text = {
            TokenGrid {
                items(heldIndices, key = { it }) { logIndex ->
                    val entry = log[logIndex]
                    HeldRewardCell(
                        token = FactionRewardTokenCatalogue.byId(entry.tokenId),
                        tokenId = entry.tokenId,
                        onOpen = { onOpenDetail(logIndex) },
                        onSpend = { onSpend(logIndex) },
                    )
                }
            }
        },
    )
}

/** One held-reward grid cell: the token's face, its name, and a Spend checkbox (issue #252). A held
 * token is by definition un-spent, so the checkbox is always the empty/outlined state - tapping it
 * spends the token. */
@Composable
private fun HeldRewardCell(token: FactionRewardToken?, tokenId: String, onOpen: () -> Unit, onSpend: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (token != null) {
            FactionRewardTokenFace(token = token, size = 72.dp)
        }
        Text(
            text = token?.name ?: tokenId,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onSpend) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Spend",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Read-only detail for a faction reward token (issue #252) - the reward piles' counterpart to
 * [RuinInfoDialog], opened by tapping a reward cell in [PileContentsDialog]'s pile survey. Same face
 * + effect + discard footer as [FactionRewardZoomDialog] but with none of its Spend action - this is
 * a preview of what's still in the pile, not a held token.
 */
@Composable
internal fun RewardInfoDialog(token: FactionRewardToken, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(token.name) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RewardBody(token = token)
            }
        },
    )
}

/**
 * Grid overview of a multi-token draw batch (D3/D7), possibly spanning several piles at once
 * (D16): one cell per drawn token with art, name and (when [showDefeatToggle]) a Defeat toggle;
 * tapping a cell opens that token's own zoomed detail with prev/next across the same batch. Sized
 * to content with a max height (D10) so a rare large batch scrolls internally instead of pushing
 * the dialog off-screen, rather than always reserving full-screen space for the common small batch.
 *
 * Reused for a Summon Draw's own multi-child result (issue #191) with [showDefeatToggle] = false -
 * a summoned child is never independently marked Defeated (`CONTEXT.md`'s "Summon Draw") - and a
 * caller-supplied [title], since that case reads "N tokens summoned" rather than "drawn". Left at
 * its default [currentChildOf] for that same call, since a child cell never has children of its own.
 */
@Composable
internal fun TokenGridDialog(
    state: GridState,
    log: List<DrawLogEntry>,
    title: String,
    showDefeatToggle: Boolean,
    onOpenDetail: (Int) -> Unit,
    onToggleDefeated: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
    currentChildOf: (Int) -> DrawLogEntry? = { null },
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(title) },
        text = {
            // Adaptive columns (not a fixed count) so 2-6 tokens - the common case - render as a
            // comfortably large grid, while a rare large batch just packs more/smaller columns
            // automatically instead of needing separate tuning (D9).
            TokenGrid {
                // position = index within this batch (for prev/next); logIndex = the entry's real
                // Draw Log index (for reading/writing its defeated flag).
                itemsIndexed(state.logIndices) { position, logIndex ->
                    TokenGridCell(
                        entry = log[logIndex],
                        showDefeatToggle = showDefeatToggle,
                        summonedChild = currentChildOf(logIndex)?.let { TokenCatalogue.byId(it.tokenId) },
                        onOpen = { onOpenDetail(position) },
                        onToggleDefeated = { defeated -> onToggleDefeated(logIndex, defeated) },
                    )
                }
            }
        },
    )
}

/** One grid cell: art + name (dimmed once defeated, matching the Draw Log's own treatment) and,
 * when [showDefeatToggle], a Defeat toggle icon matching [DrawLogRow]'s. A non-null [summonedChild]
 * (issue #191) superimposes a small thumbnail of it on the art's corner, the same "which token is
 * actually fighting" cue `CONTEXT.md`'s Possessed Enemy pairing uses. The cell's own
 * [Modifier.clickable] opens the detail view; it sits underneath the toggle's own clickable, which
 * intercepts taps on itself first, so tapping the icon toggles Defeat instead of also opening the
 * detail. */
@Composable
private fun TokenGridCell(
    entry: DrawLogEntry,
    showDefeatToggle: Boolean,
    summonedChild: EnemyToken?,
    onOpen: () -> Unit,
    onToggleDefeated: (Boolean) -> Unit,
) {
    val token = TokenCatalogue.byId(entry.tokenId)
    Column(
        // fillMaxWidth (not a fixed width) so the cell matches whatever slot width Adaptive chose
        // for this row - which can be wider than GRID_CELL_MIN_SIZE once it divides evenly.
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.alpha(if (entry.defeated) 0.5f else 1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (token != null) {
                val possessed = entry.possessedToken()
                if (possessed != null) {
                    // A possessed enemy shows its composite face (circular over the possessed token).
                    PossessedEnemyFace(circular = token, possessed = possessed, size = 72.dp)
                } else {
                    // The grid/log cell keeps a single small corner badge even for a double-summoner -
                    // both children are still reachable via the row's "Summoned: …" line and the zoom.
                    EnemyTokenFaceWithSummon(token = token, size = 72.dp, summonedChildren = listOfNotNull(summonedChild))
                }
            }
            Text(
                text = token?.name ?: entry.tokenId,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showDefeatToggle) {
            IconButton(onClick = { onToggleDefeated(!entry.defeated) }) {
                Icon(
                    imageVector = if (entry.defeated) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (entry.defeated) "Defeated" else "Mark defeated",
                    tint = if (entry.defeated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * [EnemyTokenFace] with each of [summonedChildren]'s own art superimposed (issue #191) - the same
 * "which token is actually fighting" cue `CONTEXT.md`'s Possessed Enemy pairing uses, applied to a
 * summoner once it has current Summon Draw children. No overlay when the list is empty (never
 * summoned, or not a summoner at all). [alignment] defaults to a small corner badge (grid cells,
 * Draw Log row); the zoom dialog instead pins it to the art's left side - where a printed summon
 * token shows its pile-color icon - so it reads as "this is what's standing in" rather than a
 * decorative badge.
 *
 * Several children stack in a column along that same edge, so a *double* summoner (the Lost Legion
 * Dragon Summoner, the only token with two Summon attacks) shows both replacements roughly over its
 * two printed summon slots rather than hiding one behind the other. A single child is just a
 * one-item column, identical to the pre-#188 single-badge layout.
 */
@Composable
private fun EnemyTokenFaceWithSummon(
    token: EnemyToken,
    size: Dp,
    summonedChildren: List<EnemyToken>,
    alignment: Alignment = Alignment.BottomEnd,
) {
    Box {
        EnemyTokenFace(token = token, size = size)
        if (summonedChildren.isNotEmpty()) {
            // A column centered on the aligned edge: with two children it splits into an upper and a
            // lower badge, which lands them over the summoner's two stacked summon-token slots.
            Column(
                modifier = Modifier.align(alignment),
                verticalArrangement = Arrangement.spacedBy(size * SUMMON_STACK_GAP),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                summonedChildren.forEach { child ->
                    Box(Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)) {
                        EnemyTokenFace(token = child, size = size * SUMMON_BADGE_SCALE)
                    }
                }
            }
        }
    }
}

/** The "?" info window: every whole-token ability and per-attack modifier, with its rules text. */
@Composable
internal fun TokenInfoDialog(token: EnemyToken, onDismiss: () -> Unit) {
    // Gather each ability/resistance on this token, with its description. Resistances and defensive
    // abilities describe how it's attacked; offensive abilities modify its own attacks - all are
    // whole-token (they apply to every attack), so they're listed once each, not per attack.
    val lines = buildList {
        token.resistances.forEach { add(it.displayName() + " Resistance" to "Attacks of this element are inefficient (halved).") }
        token.defensiveAbilities.forEach { add(it.describe()) }
        // Defend is a valued defensive trait (a number, not an enum), so it's described inline here
        // rather than via DefensiveAbility.describe() (Shades of Tezla, "New Enemy Token Abilities").
        token.defend?.let { add("Defend $it" to "The first enemy you attack this combat has its Armor raised by $it until the end of the combat.") }
        token.offensiveAbilities.forEach { add(it.describe()) }
        // Summon isn't an ability - it's a whole different kind of attack - so describe it here.
        token.attacks.filter { it.isSummon }.forEach {
            add("Summon" to "At the start of the Block phase, draws a ${it.summons?.summonName() ?: "token"} token to fight in its place.")
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("${token.name} · abilities") },
        text = {
            if (lines.isEmpty()) {
                Text("This enemy has no special abilities.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    lines.forEach { (name, desc) ->
                        Column {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text(desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
    )
}

/**
 * The "view draw pile" dialog (issue #231): the still-face-down contents of one [pile], grouped by
 * identity (art + a "×N" count badge) and sorted alphabetically by name via [composePile], so a
 * player can survey what they might still face and plan combat. Deliberately shows *unordered*
 * composition - never draw order - so it can't reveal the next token. Tapping an enemy cell opens
 * its ability window ([onOpenEnemyInfo]); a ruin cell opens its read-only detail ([onOpenRuinInfo]).
 * Under [withReplacement] the pile never depletes, so the title frames it as the full pile rather
 * than "remaining".
 */
@Composable
internal fun PileContentsDialog(
    pileId: TokenPileId,
    pile: TokenPile,
    withReplacement: Boolean,
    onOpenEnemyInfo: (EnemyToken) -> Unit,
    onOpenRuinInfo: (RuinToken) -> Unit,
    onOpenRewardInfo: (FactionRewardToken) -> Unit,
    onDismiss: () -> Unit,
) {
    val isRuin = pileId == TokenPileId.RUIN
    val isReward = pileId.isFactionReward
    val isPossessed = pileId == TokenPileId.POSSESSED
    // Name resolver for the sort, drawn from whichever catalogue this pile uses. A ruin has no single
    // "name" field (labelled by kind), a reward carries its own name, and a possessed token has none
    // either (labelled by its modifier summary, "+2 Armor · +3 Psychic").
    val nameOf: (String) -> String = { id ->
        when {
            isRuin -> RuinTokenCatalogue.byId(id)?.let { if (it.isAltar) "Ancient Altar" else "Enemies with Treasure" } ?: id
            isReward -> FactionRewardTokenCatalogue.byId(id)?.name ?: id
            isPossessed -> PossessedTokenCatalogue.byId(id)?.summary() ?: id
            else -> TokenCatalogue.byId(id)?.name ?: id
        }
    }
    // The face-down draw pile and the discard are composed and shown as two *separate* sections
    // (issue #251), never merged into one grid: defeated tokens reshuffle back on a Replenish, so
    // they're still tokens you might face, but they are not in the draw pile now - conflating the two
    // is exactly what made a discard look like it went straight back into the draw pile. On-board
    // tokens are in neither list (out for good until defeated). Each is shown unordered, so it never
    // reveals draw order. remember keyed per list so each grouping recomputes only when it changes.
    val drawComposition = remember(pile.drawPile) { composePile(pile.drawPile, nameOf) }
    val discardComposition = remember(pile.discardPile) { composePile(pile.discardPile, nameOf) }
    // Title still counts everything that could yet be faced (draw + discard); with replacement the
    // discard is always empty, so this is just the full pile.
    val total = drawComposition.total + discardComposition.total
    val title = if (withReplacement) "${pileId.displayName()} · full pile ($total)"
    else "${pileId.displayName()} · $total remaining"

    val onOpen: (String) -> Unit = { id ->
        // Possessed tokens have no detail dialog (they're modifiers, not enemies), so they just display.
        when {
            isRuin -> RuinTokenCatalogue.byId(id)?.let(onOpenRuinInfo)
            isReward -> FactionRewardTokenCatalogue.byId(id)?.let(onOpenRewardInfo)
            isPossessed -> Unit
            else -> TokenCatalogue.byId(id)?.let(onOpenEnemyInfo)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(title) },
        text = {
            if (drawComposition.groups.isEmpty() && discardComposition.groups.isEmpty()) {
                // Empty draw pile *and* discard means every token drawn from this pile is still on
                // the board - the pile is genuinely empty until one is defeated back into it (#251).
                Text("Nothing left to draw — every token from this pile is on the board.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Face-down draw pile: what a draw takes from right now. Under replacement this is
                    // the whole pile, so its own header would be redundant with the title - omit it.
                    if (drawComposition.groups.isNotEmpty()) {
                        if (!withReplacement) {
                            Text(
                                "Face-down draw pile · ${drawComposition.total}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        PileContentsGrid(drawComposition.groups, isRuin, isReward, isPossessed, onOpen)
                    }
                    // Discard: separate from the draw pile until a Replenish shuffles it back in.
                    if (discardComposition.groups.isNotEmpty()) {
                        Text(
                            "Discard · ${discardComposition.total} — reshuffles in when the draw pile runs out",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PileContentsGrid(discardComposition.groups, isRuin, isReward, isPossessed, onOpen)
                    }
                }
            }
        },
    )
}

/**
 * One section of the [PileContentsDialog]: an adaptive grid of a composed pile's token [groups] (the
 * same grid the draw-result overview uses, D9, for a consistent look). Extracted so the face-down
 * draw pile and the discard render identically (issue #251); [onOpen] receives the tapped token id
 * and routes it to the right detail dialog.
 */
@Composable
private fun PileContentsGrid(
    groups: List<PileTokenGroup>,
    isRuin: Boolean,
    isReward: Boolean,
    isPossessed: Boolean,
    onOpen: (String) -> Unit,
) {
    TokenGrid {
        items(groups, key = { it.tokenId }) { group ->
            PileContentsCell(
                group = group,
                isRuin = isRuin,
                isReward = isReward,
                isPossessed = isPossessed,
                onOpen = { onOpen(group.tokenId) },
            )
        }
    }
}

/**
 * One cell of the [PileContentsDialog] grid: a token's art with a "×N" badge when more than one
 * copy remains (a lone copy needs no count), its name below, tappable to open detail. Ruin ids
 * render the hexagonal [RuinTokenFace]; everything else the round [EnemyTokenFace].
 */
@Composable
private fun PileContentsCell(group: PileTokenGroup, isRuin: Boolean, isReward: Boolean, isPossessed: Boolean, onOpen: () -> Unit) {
    val token = if (isRuin || isReward || isPossessed) null else TokenCatalogue.byId(group.tokenId)
    val ruin = if (isRuin) RuinTokenCatalogue.byId(group.tokenId) else null
    val reward = if (isReward) FactionRewardTokenCatalogue.byId(group.tokenId) else null
    val possessed = if (isPossessed) PossessedTokenCatalogue.byId(group.tokenId) else null
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Box so the count badge can overlay the art's corner.
        Box(contentAlignment = Alignment.Center) {
            when {
                token != null -> EnemyTokenFace(token = token, size = 72.dp)
                ruin != null -> RuinTokenFace(ruin = ruin, size = 72.dp)
                reward != null -> FactionRewardTokenFace(token = reward, size = 72.dp)
                possessed != null -> PossessedTokenFace(possessed = possessed, size = 72.dp)
                else -> Text(group.tokenId, style = MaterialTheme.typography.labelSmall)
            }
            if (group.count > 1) {
                CountBadge(count = group.count, modifier = Modifier.align(Alignment.BottomEnd))
            }
        }
        Text(
            text = group.displayName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A small "×N" pill overlaid on a pile-contents cell when several copies of a token remain. */
@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "×$count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * Read-only detail for a ruin token (issue #231) - the RUIN pile's counterpart to [TokenInfoDialog],
 * opened by tapping a ruin cell in [PileContentsDialog]. Shows the same face and prompt as
 * [RuinZoomDialog] (altar payment + Fame, or the Enemies-With-Treasure draw line + reward) but with
 * none of its draw/Defeat actions - this is a preview of pile contents, not a drawn ruin.
 */
@Composable
internal fun RuinInfoDialog(ruin: RuinToken, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(if (ruin.isAltar) "Ancient Altar" else "Enemies with Treasure") },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RuinBody(ruin = ruin)
            }
        },
    )
}

/** The Defeat dialog: a Defeated toggle plus a free-text note (for enemies kept on the board),
 * saved together. This is the per-log-row entry point (note-editing needs the dialog); the zoom
 * and grid views instead use a one-tap Defeat button/checkbox with no note. */
@Composable
internal fun DefeatDialog(entry: DrawLogEntry, onSave: (Boolean, String) -> Unit, onDismiss: () -> Unit) {
    var defeated by remember { mutableStateOf(entry.defeated) }
    var note by remember { mutableStateOf(entry.note) }
    val token = TokenCatalogue.byId(entry.tokenId)
    val reward = if (token == null) FactionRewardTokenCatalogue.byId(entry.tokenId) else null
    val ruin = if (token == null && reward == null) RuinTokenCatalogue.byId(entry.tokenId) else null
    val title = token?.name
        ?: reward?.name
        ?: ruin?.let { if (it.isAltar) "Ancient Altar" else "Enemies with Treasure" }
        ?: entry.tokenId
    // A reward token's "defeated" flag means "spent" (issue #252); a ruin's means "resolved".
    val doneLabel = when {
        reward != null -> "Spent"
        ruin != null -> "Resolved"
        else -> "Defeated"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(defeated, note) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledSwitch(label = doneLabel, checked = defeated, onCheckedChange = { defeated = it })
                // The note is for tracking an enemy still on the board ("keep, NE tile"), so it's
                // only editable while the enemy is *not* marked defeated.
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (e.g. \"keep, NE tile\")") },
                    enabled = !defeated,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}
