package com.guyteichman.mageknightbuddy.ui.sites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guyteichman.mageknightbuddy.data.FavoriteSitesRepository
import com.guyteichman.mageknightbuddy.domain.Site
import com.guyteichman.mageknightbuddy.domain.SiteCatalogue
import com.guyteichman.mageknightbuddy.domain.SiteCategory
import com.guyteichman.mageknightbuddy.domain.SiteExpansion
import com.guyteichman.mageknightbuddy.ui.settings.SettingsAction

private const val SITES_LIST_ROUTE = "sites_list"

// "{id}" is a placeholder segment filled in with a Site.id at navigation time (see the
// "sites_detail/$id" call below). Keying the detail route on the stable id - rather than a list
// index like the Scoreboard tab uses - matters here because the list is filtered by the search box,
// so a position would point at the wrong site (or off the end) once the query changes.
private const val SITES_DETAIL_ROUTE = "sites_detail/{id}"

/**
 * Root composable for the Sites tab (issue #234): a searchable, alphabetical list of every [Site] in
 * the [SiteCatalogue] and, on tapping a row, a full-screen detail with that site's art and rules.
 * Favorited sites (issue #236) pin to a "★ Favorites" section at the top and can be starred from
 * either the row or the detail.
 *
 * Runs its own nested [NavHost] - separate from the app's top-level tab navigation, exactly like the
 * Scoreboard tab's breakdown - so pushing a detail only touches this tab's back stack, and switching
 * away to another tab and back leaves the list-vs-detail state untouched. The catalogue itself is
 * static (read from [SiteCatalogue.sites] directly), so the only state the [SitesViewModel] holds is
 * the persisted favorites, sourced from [favoritesRepository].
 */
@Composable
fun SitesTab(favoritesRepository: FavoriteSitesRepository, onOpenSettings: () -> Unit) {
    // A NavController scoped to this tab's own nested graph, distinct from the app's tab-switching one.
    val nestedNavController = rememberNavController()
    // The tab's one ViewModel: scoped to this tab, so it (and the favorites it exposes) outlives
    // navigating into and back out of a detail, keeping the star state in sync between list and detail.
    val viewModel: SitesViewModel = viewModel(factory = SitesViewModel.factory(favoritesRepository))
    // collectAsState turns the favorites Flow into Compose state (emptySet until Room's first
    // emission), so toggling a star anywhere recomposes both the list and any open detail.
    val favorites by viewModel.favorites.collectAsState(initial = emptySet())

    NavHost(navController = nestedNavController, startDestination = SITES_LIST_ROUTE) {
        composable(SITES_LIST_ROUTE) {
            SitesListScreen(
                favorites = favorites,
                // Method reference: (id, favorite) -> Unit, matching SitesViewModel.setFavorite.
                onToggleFavorite = viewModel::setFavorite,
                onSiteClick = { id -> nestedNavController.navigate("sites_detail/$id") },
                onOpenSettings = onOpenSettings,
            )
        }
        composable(
            SITES_DETAIL_ROUTE,
            // Declares "{id}" as a String argument so it can be read back out below.
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            // byId returns null for an unknown id (shouldn't happen from our own navigation, but is
            // the safe thing to do); ?.let only renders the detail when a site actually exists.
            id?.let { SiteCatalogue.byId(it) }?.let { site ->
                SiteDetailScreen(
                    site = site,
                    // `in` on the collected Set is an O(1) membership test (see FavoriteSitesRepository).
                    isFavorite = site.id in favorites,
                    onToggleFavorite = viewModel::setFavorite,
                    onBack = { nestedNavController.popBackStack() },
                )
            }
        }
    }
}

/**
 * The list screen: a search field and the group/filter controls pinned above a scrolling list of
 * matching sites (issues #234, #237, #236). Search query, grouping mode, and the two filter
 * selections are all hoisted here as `rememberSaveable` state; [favorites] (the currently starred
 * ids) comes down from the ViewModel, and toggling a row's star calls [onToggleFavorite]. Tapping a
 * row hands its id up to [onSiteClick]. The visible, sectioned list - including the pinned "★
 * Favorites" section - is recomputed by [searchedFilteredGrouped] whenever any input (favorites
 * included) changes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SitesListScreen(
    favorites: Set<String>,
    onToggleFavorite: (String, Boolean) -> Unit,
    onSiteClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    // rememberSaveable keeps each control's state across configuration changes (rotation) and tab
    // switches (the top-level nav saves/restores this tab's state). Every value here is
    // Parcelable-safe on its own - a String, an enum, and Sets of enums (enums serialize by name) -
    // so no custom Saver is needed.
    var query by rememberSaveable { mutableStateOf("") }
    var grouping by rememberSaveable { mutableStateOf(SiteGrouping.NONE) }
    var selectedExpansions by rememberSaveable { mutableStateOf(emptySet<SiteExpansion>()) }
    var selectedCategories by rememberSaveable { mutableStateOf(emptySet<SiteCategory>()) }
    // Whether the filter bottom sheet is open. Saveable so an open sheet survives rotation.
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    // remember(...) recomputes the grouped result only when one of these inputs changes, not on every
    // unrelated recomposition. The catalogue itself is static, so these are the only inputs that vary.
    val groups = remember(query, grouping, selectedExpansions, selectedCategories, favorites) {
        SiteCatalogue.sites.searchedFilteredGrouped(query, selectedExpansions, selectedCategories, grouping, favorites)
    }
    // Total active filter chips across both axes - drives the badge on the Filter button.
    val activeFilterCount = selectedExpansions.size + selectedCategories.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sites") },
                // The shared settings gear, present on every tab's top bar so Settings is reachable
                // from anywhere without its own bottom-nav tab (see SettingsAction).
                actions = { SettingsAction(onClick = onOpenSettings) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                // A clear (X) button only while there's something to clear.
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                placeholder = { Text("Search sites") },
            )

            // Group-by dropdown on the left, Filter button (with active-count badge) on the right.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GroupByControl(grouping = grouping, onSelect = { grouping = it })
                // weight pushes the Filter button to the trailing edge.
                Spacer(Modifier.weight(1f))
                FilterButton(activeCount = activeFilterCount, onClick = { showFilterSheet = true })
            }

            if (groups.isEmpty()) {
                // Generalized empty state: name the query when there is one, otherwise blame the filters
                // (the only other thing that can empty the list now).
                val message =
                    if (query.isNotBlank()) "No sites match “$query”" else "No sites match the selected filters"
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(message)
                }
            } else {
                // Whether a "★ Favorites" section is being shown. Drives the flat-list remainder header
                // below: the favorites section is only meaningful if the rest is *also* labelled, so
                // there's a visible boundary between the two (issue #236 follow-up).
                val hasFavoritesSection = groups.any { it.isFavorites }
                // LazyColumn only composes the rows on screen, so the whole catalogue isn't laid out at once.
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Each group contributes a sticky header (see the cases below) followed by its rows.
                    // Site ids are unique across the whole catalogue, so they stay valid LazyColumn keys
                    // even spanning several groups.
                    groups.forEach { group ->
                        val key = group.key
                        when {
                            // The pinned favorites section (issue #236): a fixed "★ Favorites" header.
                            // Sticky, like every other header - it always has a following header to hand
                            // the pinned slot off to as you scroll past it (a category/expansion header
                            // when grouped, or the "Other Sites" header below when flat), so it never
                            // stays stuck over non-favorite rows.
                            group.isFavorites -> stickyHeader(key = "header_favorites") {
                                GroupHeader(label = "★ Favorites", count = group.sites.size)
                            }
                            key != null -> stickyHeader(key = "header_${key.name}") {
                                GroupHeader(label = key.siteGroupHeader(), count = group.sites.size)
                            }
                            // The flat (Group: None) remainder. It only gets a header - "Other Sites" -
                            // when a favorites section sits above it, so the two are clearly separated;
                            // a plain no-favorites flat list stays header-less as before (this branch is
                            // reached only for the null-keyed, non-favorites group, which exists only in
                            // Group: None).
                            hasFavoritesSection -> stickyHeader(key = "header_other") {
                                GroupHeader(label = "Other Sites", count = group.sites.size)
                            }
                        }
                        items(group.sites, key = { it.id }) { site ->
                            SiteRow(
                                site = site,
                                isFavorite = site.id in favorites,
                                onToggleFavorite = onToggleFavorite,
                                onClick = { onSiteClick(site.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    // The filter sheet is a modal overlay, so it sits outside the Column and is shown conditionally.
    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }, sheetState = sheetState) {
            FilterSheetContent(
                selectedExpansions = selectedExpansions,
                selectedCategories = selectedCategories,
                // toggled() flips one chip in/out of the selection Set (see below).
                onToggleExpansion = { selectedExpansions = selectedExpansions.toggled(it) },
                onToggleCategory = { selectedCategories = selectedCategories.toggled(it) },
                onReset = {
                    selectedExpansions = emptySet()
                    selectedCategories = emptySet()
                },
            )
        }
    }
}

/**
 * The always-visible "Group by" control: an outlined button showing the current [SiteGrouping] that
 * opens a small [DropdownMenu] of the three options. A dropdown (rather than a segmented button row)
 * keeps this compact on a narrow phone and leaves room for the Filter button beside it.
 */
@Composable
private fun GroupByControl(grouping: SiteGrouping, onSelect: (SiteGrouping) -> Unit) {
    // Local open/closed state for the menu - pure UI, so plain remember (not hoisted state).
    var expanded by remember { mutableStateOf(false) }
    // Box so the DropdownMenu anchors to the button below it.
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("Group: ${grouping.label()}")
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SiteGrouping.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * The Filter button: opens the filter bottom sheet, with a [Badge] showing how many filter chips are
 * active ([activeCount]) so the user can tell at a glance the list is narrowed even with the sheet
 * closed. No badge is drawn when nothing is filtered.
 */
@Composable
private fun FilterButton(activeCount: Int, onClick: () -> Unit) {
    // BadgedBox overlays the count bubble on the top-end corner of its content (the button).
    BadgedBox(
        badge = { if (activeCount > 0) Badge { Text("$activeCount") } },
    ) {
        OutlinedButton(onClick = onClick) {
            Icon(Icons.Filled.FilterList, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Filter")
        }
    }
}

/**
 * Contents of the filter bottom sheet: two wrapping rows of [FilterChip]s (one per [SiteExpansion],
 * one per [SiteCategory]) plus a Reset action. A selected chip narrows the list; nothing selected in
 * an axis leaves that axis unconstrained (see [searchedFilteredGrouped]). Reset is disabled when
 * there's nothing to clear.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheetContent(
    selectedExpansions: Set<SiteExpansion>,
    selectedCategories: Set<SiteCategory>,
    onToggleExpansion: (SiteExpansion) -> Unit,
    onToggleCategory: (SiteCategory) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Filter", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(
                onClick = onReset,
                // Nothing to reset when both axes are already empty.
                enabled = selectedExpansions.isNotEmpty() || selectedCategories.isNotEmpty(),
            ) { Text("Reset") }
        }

        Text("Expansion", style = MaterialTheme.typography.labelLarge)
        // FlowRow wraps chips onto extra lines instead of squeezing them; entries preserves enum order.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SiteExpansion.entries.forEach { expansion ->
                FilterChip(
                    selected = expansion in selectedExpansions,
                    onClick = { onToggleExpansion(expansion) },
                    label = { Text(expansion.badgeLabel()) },
                )
            }
        }

        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SiteCategory.entries.forEach { category ->
                FilterChip(
                    selected = category in selectedCategories,
                    onClick = { onToggleCategory(category) },
                    label = { Text(category.groupLabel()) },
                )
            }
        }
    }
}

/**
 * A sticky section header for the grouped list: the group's [label] and how many sites it holds
 * ([count]). Drawn on an opaque [Surface] so list rows don't show through while it's pinned to the top.
 */
@Composable
private fun GroupHeader(label: String, count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

/** Returns a copy of this set with [value] toggled: removed if present, added otherwise. */
private fun <T> Set<T>.toggled(value: T): Set<T> = if (value in this) this - value else this + value

/** Short label for the group-by control and its menu items. */
private fun SiteGrouping.label(): String = when (this) {
    SiteGrouping.NONE -> "None"
    SiteGrouping.CATEGORY -> "Category"
    SiteGrouping.EXPANSION -> "Expansion"
}

/**
 * Header text for a [SiteGroup]'s key. A group is keyed on either a [SiteCategory] or a
 * [SiteExpansion]; each has its own display string (categories get a pluralized [groupLabel],
 * expansions reuse the row [badgeLabel]). The `else` can't occur - those are the only two key
 * types - but keeps the `when` exhaustive over the open `Enum<*>` type.
 */
private fun Enum<*>.siteGroupHeader(): String = when (this) {
    is SiteCategory -> groupLabel()
    is SiteExpansion -> badgeLabel()
    else -> name
}

/**
 * Human-readable, pluralized label for a [SiteCategory] - used as a group header and a filter-chip
 * label. Kept here (a UI concern) rather than on the enum, mirroring [badgeLabel].
 */
private fun SiteCategory.groupLabel(): String = when (this) {
    SiteCategory.RAMPAGING_ENEMY -> "Rampaging Enemies"
    SiteCategory.FORTIFIED_SITE -> "Fortified Sites"
    SiteCategory.ADVENTURE_SITE -> "Adventure Sites"
    SiteCategory.SETTLEMENT -> "Settlements"
    SiteCategory.RESOURCE_SITE -> "Resource Sites"
    SiteCategory.SPECIAL_TILE -> "Special Tiles"
    SiteCategory.TERRAIN_FEATURE -> "Terrain Features"
}

/**
 * One list row: art thumbnail + name, a small expansion badge, and a trailing star toggle (issue
 * #236). Tapping the row opens the detail; tapping the star instead toggles the favorite - the star's
 * own [IconButton] consumes that tap, so it doesn't also fire the row's click. [isFavorite] chooses
 * the filled vs outlined star; [onToggleFavorite] is called with the site id and the desired new state.
 */
@Composable
private fun SiteRow(
    site: Site,
    isFavorite: Boolean,
    onToggleFavorite: (String, Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                // No end padding: the trailing star's IconButton carries its own touch padding, which
                // supplies the right-edge spacing instead.
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SiteThumbnail(site = site)
            Spacer(Modifier.width(16.dp))
            // weight(1f) lets the name take the leftover width and push the badge + star to the right edge.
            Text(
                text = site.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            ExpansionBadge(site.expansion)
            FavoriteStar(isFavorite = isFavorite, onClick = { onToggleFavorite(site.id, !isFavorite) })
        }
        HorizontalDivider()
    }
}

/**
 * The star toggle shown on both a list row and the detail top bar (issue #236): a filled, primary-
 * tinted star when [isFavorite], an outlined one in the default content color otherwise. [onClick]
 * flips the state. The content description flips too, so a screen reader announces the *action*
 * ("Favorite" / "Unfavorite"), not just "star".
 */
@Composable
private fun FavoriteStar(isFavorite: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
            contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
            tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        )
    }
}

/**
 * Full-screen detail for one site, pushed by tapping a row: a large art header, the expansion badge,
 * then every [com.guyteichman.mageknightbuddy.domain.SiteSection] in printed order. The back arrow
 * pops this off the nested NavHost's back stack. A star action in the top bar (issue #236) toggles
 * this site's favorite; [isFavorite] draws its filled/outlined state and [onToggleFavorite] is called
 * with the desired new state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiteDetailScreen(
    site: Site,
    isFavorite: Boolean,
    onToggleFavorite: (String, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(site.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                // Same star toggle as the list row, here as a top-bar action.
                actions = {
                    FavoriteStar(isFavorite = isFavorite, onClick = { onToggleFavorite(site.id, !isFavorite) })
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // rememberScrollState keeps the scroll position across recompositions; long sites
                // (Maze, Labyrinth) run past one screen.
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SiteArtHeader(site)
            ExpansionBadge(site.expansion)
            // One heading+body block per section, in the catalogue's printed order.
            site.sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = section.heading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = section.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** A small rounded pill naming the [SiteExpansion] a site comes from - the only per-site metadata shown. */
@Composable
private fun ExpansionBadge(expansion: SiteExpansion) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = expansion.badgeLabel(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** Human-readable label for the expansion badge. Kept here (UI concern) rather than on the enum. */
private fun SiteExpansion.badgeLabel(): String = when (this) {
    SiteExpansion.BASE -> "Base"
    SiteExpansion.LOST_LEGION -> "Lost Legion"
    SiteExpansion.SHADES_OF_TEZLA -> "Shades of Tezla"
    SiteExpansion.APOCALYPSE_DRAGON -> "Apocalypse Dragon"
}
