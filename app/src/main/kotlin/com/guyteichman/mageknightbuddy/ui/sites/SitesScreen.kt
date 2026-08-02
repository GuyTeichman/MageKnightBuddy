package com.guyteichman.mageknightbuddy.ui.sites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guyteichman.mageknightbuddy.domain.Site
import com.guyteichman.mageknightbuddy.domain.SiteCatalogue
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
 *
 * Runs its own nested [NavHost] - separate from the app's top-level tab navigation, exactly like the
 * Scoreboard tab's breakdown - so pushing a detail only touches this tab's back stack, and switching
 * away to another tab and back leaves the list-vs-detail state untouched. The catalogue is static, so
 * unlike the other tabs this needs no ViewModel or repository: it reads [SiteCatalogue.sites] directly.
 */
@Composable
fun SitesTab(onOpenSettings: () -> Unit) {
    // A NavController scoped to this tab's own nested graph, distinct from the app's tab-switching one.
    val nestedNavController = rememberNavController()

    NavHost(navController = nestedNavController, startDestination = SITES_LIST_ROUTE) {
        composable(SITES_LIST_ROUTE) {
            SitesListScreen(
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
                SiteDetailScreen(site = site, onBack = { nestedNavController.popBackStack() })
            }
        }
    }
}

/**
 * The list screen: a search field pinned above a scrolling list of matching sites. Search query and
 * the filtered result are hoisted here; tapping a row hands its id up to [onSiteClick].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SitesListScreen(onSiteClick: (String) -> Unit, onOpenSettings: () -> Unit) {
    // rememberSaveable keeps the typed query across configuration changes (rotation) and across
    // tab switches (the top-level nav saves/restores this tab's state), so returning to Sites keeps
    // whatever was being searched.
    var query by rememberSaveable { mutableStateOf("") }

    // remember(query) recomputes the filtered+sorted list only when the query changes, not on every
    // unrelated recomposition. The catalogue itself is static, so it's the only input that varies.
    val results = remember(query) { SiteCatalogue.sites.searchedAndSorted(query) }

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

            if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sites match “$query”")
                }
            } else {
                // LazyColumn only composes the rows on screen, so the whole catalogue isn't laid out at once.
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // key = each site's id so Compose reuses the right row across filtering, rather than
                    // re-composing by position when the result set changes.
                    items(results, key = { it.id }) { site ->
                        SiteRow(site = site, onClick = { onSiteClick(site.id) })
                    }
                }
            }
        }
    }
}

/** One list row: art thumbnail + name, with a small expansion badge trailing. Tapping opens the detail. */
@Composable
private fun SiteRow(site: Site, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SiteThumbnail(site = site)
            Spacer(Modifier.width(16.dp))
            // weight(1f) lets the name take the leftover width and push the badge to the right edge.
            Text(
                text = site.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            ExpansionBadge(site.expansion)
        }
        HorizontalDivider()
    }
}

/**
 * Full-screen detail for one site, pushed by tapping a row: a large art header, the expansion badge,
 * then every [com.guyteichman.mageknightbuddy.domain.SiteSection] in printed order. The back arrow
 * pops this off the nested NavHost's back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiteDetailScreen(site: Site, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(site.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
