package com.bothybag.app.data

/**
 * Links MBA news articles to the bothy they are about, by looking for a bothy
 * name in the article title. Plain Kotlin with no Android dependencies, so it
 * can be unit tested.
 */
object NewsMatcher {

    /**
     * Bothy names that are also everyday words or place names once the "Bothy"
     * suffix is dropped. These only match when the full name appears in the title.
     */
    private val AMBIGUOUS_SHORT_NAMES = setOf("green", "oban")

    /**
     * Normalize a string for fuzzy matching: lowercase, strip apostrophes/special
     * chars, collapse whitespace.
     */
    fun normalize(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[''`\u2018\u2019\u201A\u201B]"), "") // strip apostrophes
            .replace(Regex("[^a-z0-9\\s]"), " ")                 // non-alphanumeric → space
            .replace(Regex("\\s+"), " ")                         // collapse whitespace
            .trim()
    }

    /**
     * Match news items to MBA bothies by checking if a bothy name appears in the title
     * as whole words. Strips "Bothy" suffix, apostrophes, and special characters for
     * flexible matching. Only MBA bothies are considered: the feed is the MBA's own
     * news, and non-MBA names such as "Bothy" or "Red Bothy" caused false matches.
     */
    fun match(items: List<NewsItem>, bothies: List<Bothy>): List<NewsItem> {
        val mbaBothies = bothies.filter { it.isMba }

        // Build lookup: list of (normalized name, bothy) pairs
        val bothyLookup = mbaBothies.flatMap { bothy ->
            val names = mutableListOf(normalize(bothy.name))
            // Also try without "Bothy" suffix
            val stripped = normalize(
                bothy.name.replace(Regex("\\s+bothy$", RegexOption.IGNORE_CASE), "")
            )
            if (stripped != names[0] && stripped.isNotBlank() &&
                stripped !in AMBIGUOUS_SHORT_NAMES
            ) {
                names.add(stripped)
            }
            names.filter { it.isNotBlank() }.map { it to bothy }
        }
        // Sort by name length descending so longer names match first (avoids partial matches)
        val sortedLookup = bothyLookup.sortedByDescending { it.first.length }

        return items.map { item ->
            // Pad with spaces so names only match on whole-word boundaries
            val titlePadded = " ${normalize(item.title)} "
            val match = sortedLookup.firstOrNull { (name, _) ->
                // The second check covers possessives: "Shenavall's" normalizes to "shenavalls"
                titlePadded.contains(" $name ") || titlePadded.contains(" ${name}s ")
            }
            if (match != null) {
                item.copy(
                    matchedBothyId = match.second.id,
                    matchedBothyName = match.second.name
                )
            } else {
                item
            }
        }
    }
}
