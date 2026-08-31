package com.guyteichman.mageknightbuddy.ui.scenarioart

import com.guyteichman.mageknightbuddy.domain.Scenario

/**
 * The licence a bundled scenario image is used under.
 *
 * Scenario art is sourced free-to-use so it doesn't add to the bundled-official-art debt (ADR-0010).
 * The clean licences are [PUBLIC_DOMAIN] / [CC0] (no attribution legally required - a credit is a
 * courtesy) and [CC_BY] (attribution legally *required*). In practice the whole shipped slate is
 * public-domain fine-art scans plus one CC0 museum scan.
 *
 * [OFFICIAL] is the one deliberate exception: two scenarios reuse © WizKids *Mage Knight* art (the
 * Apocalypse Dragon and Lost Legion expansions), where no clean image reads as unambiguously as the
 * real villain/antagonist. That's a knowing risk-acceptance under ADR-0010, not a clean licence -
 * those two are already covered by the Settings > Credits WizKids attribution + non-affiliation
 * disclaimer, so the courtesy credit list skips them (see `SettingsScreen`).
 *
 * Every entry still carries an [ScenarioArtEntry.author] and [ScenarioArtEntry.sourceUrl] regardless
 * of licence, so the credit line and licence check need no re-sourcing.
 */
enum class ArtLicense {
    /** Creative Commons Zero: a public-domain dedication. Credit is a courtesy, not required. */
    CC0,

    /** Creative Commons Attribution: visible attribution is legally required. */
    CC_BY,

    /** Public domain (here, by age - the artist died long enough ago). Credit is a courtesy. */
    PUBLIC_DOMAIN,

    /** Official © WizKids *Mage Knight* art, bundled under ADR-0010's risk-accepted stance. */
    OFFICIAL,
}

/**
 * One scenario's background art plus the attribution needed to use it. Kept app-side (not on the
 * pure-Kotlin [Scenario]) because art is presentation, mirroring `KnightShieldIcon`'s app-side
 * mapping - the `domain` module stays free of image/credit concerns.
 *
 * @param scenarioId the [Scenario.id] this art illustrates (a stable persistence key, so the mapping
 *   survives a scenario being renamed in the UI).
 * @param filename the image under `app/src/main/assets/scenario-art/`.
 * @param workTitle the artwork's own title (e.g. "Aurora Borealis (1865)"), shown in the credit line.
 * @param author who made the image, for the credit line.
 * @param sourceUrl where it came from, for the credit link and licence verification.
 * @param license the licence it's used under (see [ArtLicense]).
 */
data class ScenarioArtEntry(
    val scenarioId: String,
    val filename: String,
    val workTitle: String,
    val author: String,
    val sourceUrl: String,
    val license: ArtLicense,
)

/**
 * App-side catalogue mapping each [Scenario] to its background art (issue #288 filled it, on the
 * issue #285 foundation). Each row names a bundled image plus the attribution to display; the two UI
 * slices (scoreboard cards #286, picker #287) read it through [artFor] and show the real art in
 * place of [ScenarioArt]'s bronze placeholder.
 *
 * The slate is public-domain fine art (Friedrich, Turner, Church, Cole, Cropsey, Waterhouse, John
 * Martin, Hubert Robert, N. C. Wyeth, Marszewski, Dmochowski) plus one CC0 museum scan, with two
 * [ArtLicense.OFFICIAL] WizKids pieces where the real villain reads best (see [ArtLicense]).
 *
 * `object` is a Kotlin singleton, so this is a single shared catalogue with no instance to construct.
 */
object ScenarioArtCatalogue {
    // Authoritative WizKids landing page, cited as the source for the two OFFICIAL rows rather than
    // the CDN/fan-wiki the files were actually grabbed from (per ADR-0010 / issue #288).
    private const val WIZKIDS_URL = "https://wizkids.com/mage-knight/"

    /**
     * Every illustrated scenario, in [Scenario.entries] order. One row per scenario id; the filename
     * is `<scenarioId>.jpg` under `assets/scenario-art/`. `ScenarioArtAssetsTest` keeps this list and
     * the bundled files in exact sync, and `ScenarioArtCatalogueTest` checks the ids/fields.
     */
    val entries: List<ScenarioArtEntry> = listOf(
        ScenarioArtEntry(
            scenarioId = Scenario.SoloConquest.id,
            filename = "solo_conquest.jpg",
            workTitle = "Ruins of the Trakai Island Castle at Sunset (1866)",
            author = "Józef Marszewski",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:J%C3%B3zef_Marszewski_-_Ruins_of_the_Trakai_Island_Castle_at_sunset_-_MP_2685_-_National_Museum_in_Warsaw.jpg",
            license = ArtLicense.PUBLIC_DOMAIN,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.FirstReconnaissance.id,
            filename = "first_reconnaissance.jpg",
            workTitle = "Wanderer above the Sea of Fog (1818)",
            author = "Caspar David Friedrich",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Caspar_David_Friedrich_-_Wanderer_above_the_sea_of_fog.jpg",
            license = ArtLicense.PUBLIC_DOMAIN,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.ForTheCouncil.id,
            filename = "for_the_council.jpg",
            workTitle = "The Magic Circle (1886)",
            author = "John William Waterhouse",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:The_magic_circle,_by_John_William_Waterhouse.jpg",
            license = ArtLicense.PUBLIC_DOMAIN,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.HiddenValley.id,
            filename = "hidden_valley.jpg",
            workTitle = "Autumn — On the Hudson River (1860)",
            author = "Jasper Francis Cropsey",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Autumn--On_the_Hudson_River-1860-Jasper_Francis_Cropsey.jpg",
            license = ArtLicense.PUBLIC_DOMAIN,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.RealmOfTheDead.id,
            filename = "realm_of_the_dead.jpg",
            workTitle = "Crusaders Attacking the Castle of Punia",
            author = "Wincenty Dmochowski",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Wincenty_Dmochowski_-_Crusaders_attacking_the_Castle_of_Punia.jpg",
            license = ArtLicense.PUBLIC_DOMAIN,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.AgainstTheDragon.id,
            filename = "against_the_dragon.jpg",
            workTitle = "Apocalypse Dragon expansion art",
            author = "WizKids (art by Gong Studios)",
            sourceUrl = WIZKIDS_URL,
            license = ArtLicense.OFFICIAL,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.AgainstTheHorsemen.id,
            filename = "against_the_horsemen.jpg",
            workTitle = "The Spirit of War (1851)",
            author = "Jasper Francis Cropsey",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Jasper_Francis_Cropsey,_The_Spirit_of_War,_1851,_NGA_56598.jpg",
            license = ArtLicense.CC0,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.ApocalypseIsHere.id,
            filename = "apocalypse_is_here.jpg",
            workTitle = "The Great Day of His Wrath (1851–1853)",
            author = "John Martin",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:John_Martin_-_The_Great_Day_of_His_Wrath_-_Google_Art_Project.jpg",
            license = ArtLicense.PUBLIC_DOMAIN,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.FracturedLands.id,
            filename = "the_fractured_lands.jpg",
            workTitle = "Aurora Borealis (1865)",
            author = "Frederic Edwin Church",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Frederic_Edwin_Church_-_Aurora_Borealis_-_Google_Art_Project.jpg",
            license = ArtLicense.PUBLIC_DOMAIN,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.LifeAndDeath.id,
            filename = "life_and_death.jpg",
            workTitle = "Cotopaxi (1862)",
            author = "Frederic Edwin Church",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Cotopaxi_church.jpg",
            license = ArtLicense.PUBLIC_DOMAIN,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.LostRelic.id,
            filename = "lost_relic.jpg",
            workTitle = "The Course of Empire — Desolation (1836)",
            author = "Thomas Cole",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Cole_Thomas_The_Course_of_Empire_Desolation_1836.jpg",
            license = ArtLicense.PUBLIC_DOMAIN,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.AgainstTheApocalypse.id,
            filename = "against_the_apocalypse.jpg",
            workTitle = "Capriccio with the Pyramid of Maupertuis (1798)",
            author = "Hubert Robert",
            sourceUrl = "https://www.sothebys.com/en/buy/auction/2025/elegance-wonder-masterpieces-from-the-collection-of-jordan-and-thomas-a-saunders-iii/capriccio-with-the-pyramid-of-maupertuis",
            license = ArtLicense.PUBLIC_DOMAIN,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.SoloConquestChallenge.id,
            filename = "solo_conquest_challenge.jpg",
            workTitle = "Dolbadern Castle (1800)",
            author = "J. M. W. Turner",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Turner_-_Dolbadern_Castle,_1800,_031383.jpg",
            license = ArtLicense.PUBLIC_DOMAIN,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.VolkaresQuest.id,
            filename = "volkares_quest.jpg",
            workTitle = "Illustration for The Boy's King Arthur (1917)",
            author = "N. C. Wyeth",
            sourceUrl = "https://artvee.com/dl/i-am-sir-launcelot-du-lake-king-bans-son-of-benwick-and-knight-of-the-round-table/",
            license = ArtLicense.PUBLIC_DOMAIN,
        ),
        ScenarioArtEntry(
            scenarioId = Scenario.VolkaresReturn.id,
            filename = "volkares_return.jpg",
            workTitle = "Mage Knight: Lost Legion expansion art",
            author = "WizKids",
            sourceUrl = WIZKIDS_URL,
            license = ArtLicense.OFFICIAL,
        ),
    )

    // associateBy builds a scenarioId -> entry lookup once, so artFor is a map read rather than a
    // linear scan of entries on every call (this is read on every scoreboard row / picker item).
    private val byScenarioId: Map<String, ScenarioArtEntry> = entries.associateBy { it.scenarioId }

    /**
     * The art for [scenario], or null while it's still unillustrated - in which case the caller
     * ([ScenarioArt]) shows the placeholder. Keyed on the stable [Scenario.id], not the object.
     */
    fun artFor(scenario: Scenario): ScenarioArtEntry? = byScenarioId[scenario.id]
}
