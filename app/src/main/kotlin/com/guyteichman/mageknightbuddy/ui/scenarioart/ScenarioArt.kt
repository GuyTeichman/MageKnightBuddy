package com.guyteichman.mageknightbuddy.ui.scenarioart

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guyteichman.mageknightbuddy.domain.Scenario

/**
 * A darkening scrim laid over the art so overlaid text (score, name, W/L pill) stays legible on
 * bright images - part of the "render-time cohesion" that makes art from different sources read as
 * one surface. Not applied to the placeholder, which is already dark enough for its cream text.
 */
private val ART_SCRIM = Color.Black.copy(alpha = 0.35f)

/** The placeholder's metallic-bronze gradient - a warm, on-theme stand-in rather than a grey box. */
private val PLACEHOLDER_TOP = Color(0xFF7A5A34)
private val PLACEHOLDER_BOTTOM = Color(0xFF43301C)

private val DEFAULT_SHAPE = RoundedCornerShape(12.dp)

/** Width of the thin frame drawn around scenario art and its sibling chrome. */
internal val ART_FRAME_WIDTH = 1.dp

/**
 * Shared cream ink for text/icons drawn over scenario art (issue #323 - previously duplicated as
 * three separately-declared constants, two of which had drifted to disagreeing hex values).
 * [ScenarioArt] darkens the image with its own scrim, so this light colour stays legible on any
 * scenario's background - the same cream the bronze [ScenarioArtPlaceholder] uses, and also drawn
 * by [com.guyteichman.mageknightbuddy.ui.scenarioart.ScenarioPickerField] and the Scoreboard's
 * session cards. `internal` so those other packages can import it.
 */
internal val OnScenarioArtInk = Color(0xFFF3E7D3)

/**
 * The frame colour (issue #286/#287 review), resolved from the theme so it flips with dark/light:
 * MaterialTheme's `onSurface` is near-black on a light surface and near-white on a dark one, so a
 * low-alpha tint of it draws a dark outline in light mode and a light one in dark mode - staying
 * visible in both (a single fixed dark outline vanished against the dark-theme background). Exposed
 * so the scoreboard's Won/Lost pill and knight-avatar ring share the exact same colour, echoing the
 * enemy/reward-token art outline. `internal` so `ScoreboardScreen` (a different package) can reuse it.
 */
@Composable
internal fun artFrameColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

/** The standard [ScenarioArt] frame as a ready-made [BorderStroke] (see [artFrameColor]). */
@Composable
internal fun scenarioArtFrame(): BorderStroke = BorderStroke(ART_FRAME_WIDTH, artFrameColor())

/**
 * Draws a [Scenario]'s background art with render-time cohesion, so a set of images from different
 * sources reads as one coherent surface: [ContentScale.Crop] fills the caller's box (no
 * letterboxing), and a darkening [ART_SCRIM] - laid above the art but below [content] - keeps
 * overlaid text legible.
 *
 * Degrades to a bronze [ScenarioArtPlaceholder] naming the scenario for any scenario whose art
 * isn't sourced yet - which is *every* scenario at the issue #285 foundation stage, so the
 * placeholder is what actually ships until issue #288 supplies images and fills
 * [ScenarioArtCatalogue].
 *
 * The caller sizes this via [modifier] (e.g. `Modifier.fillMaxWidth().height(96.dp)` for a
 * scoreboard card, `Modifier.size(72.dp)` for a picker thumbnail); the art and its overlays fill
 * that box. [content] is a [BoxScope] slot drawn on top, so callers position the score, name, and
 * W/L pill using `Modifier.align(...)`.
 *
 * @param shape the clip shape for the whole card (defaults to a rounded rectangle).
 * @param border an optional thin outline drawn at the [shape]'s edge (pass [scenarioArtFrame] for the
 *   standard framed-tile look); null for a frameless, full-bleed image (e.g. a full-width banner).
 */
@Composable
fun ScenarioArt(
    scenario: Scenario,
    modifier: Modifier = Modifier,
    shape: Shape = DEFAULT_SHAPE,
    border: BorderStroke? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val bitmap = rememberScenarioBitmap(scenario)
    Box(
        modifier = modifier
            .clip(shape)
            // Frame drawn at the clipped edge (over the art) when the caller opts in - `.then` chains
            // the border only if one was given, since Modifier is the no-op identity otherwise.
            .then(if (border != null) Modifier.border(border, shape) else Modifier),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = scenario.displayName,
                contentScale = ContentScale.Crop,
                // matchParentSize fills the Box (whose size the caller's modifier fixed) without
                // itself influencing that size - the standard Compose "background layer" idiom.
                modifier = Modifier.matchParentSize(),
            )
            Box(Modifier.matchParentSize().background(ART_SCRIM))
        } else {
            ScenarioArtPlaceholder(scenario = scenario, modifier = Modifier.matchParentSize())
        }
        content()
    }
}

/**
 * The stand-in shown until a scenario's real art lands: a bronze gradient panel with the scenario
 * name centred, so the picker and scoreboard read as themed cards rather than blank boxes before
 * art exists. This is what ships for every scenario at the issue #285 stage.
 */
@Composable
internal fun ScenarioArtPlaceholder(scenario: Scenario, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            // A vertical bronze gradient reads as brushed metal rather than a flat fill.
            Brush.verticalGradient(listOf(PLACEHOLDER_TOP, PLACEHOLDER_BOTTOM)),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = scenario.displayName,
            color = OnScenarioArtInk,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

/**
 * Decodes a scenario's art bitmap once per art filename and caches it across recompositions. Returns
 * null when the scenario has no catalogue entry yet (the foundation state) or the asset is missing -
 * either way the caller shows the placeholder. Keyed on the filename so an unillustrated scenario
 * (key = null) doesn't try to decode, and scrolling a list doesn't re-decode.
 */
@Composable
private fun rememberScenarioBitmap(scenario: Scenario): ImageBitmap? {
    val context = LocalContext.current
    val filename = ScenarioArtCatalogue.artFor(scenario)?.filename
    return remember(filename) { filename?.let { loadScenarioBitmap(context, it) } }
}

/** Loads `scenario-art/<filename>` from assets, or null if that asset isn't bundled yet. */
private fun loadScenarioBitmap(context: Context, filename: String): ImageBitmap? = try {
    // assets.open throws if the file is absent - that's the "no art yet" signal, caught below.
    context.assets.open("scenario-art/$filename").use { stream ->
        BitmapFactory.decodeStream(stream)?.asImageBitmap()
    }
} catch (_: Exception) {
    null
}
