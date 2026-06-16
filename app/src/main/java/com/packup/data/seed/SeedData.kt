package com.packup.data.seed

import com.packup.data.local.entity.CategoryEntity
import com.packup.data.local.entity.FamilyMemberEntity
import com.packup.data.local.entity.MorningItemEntity
import com.packup.data.local.entity.PackingItemEntity

object SeedData {

    val categories = listOf(
        CategoryEntity(id = "cat-clothing", name = "Clothing", iconKey = "\uD83D\uDC55", sortOrder = 0),
        CategoryEntity(id = "cat-toiletries", name = "Toiletries", iconKey = "\uD83E\uDDF4", sortOrder = 1),
        CategoryEntity(id = "cat-misc", name = "Misc", iconKey = "\uD83D\uDCE6", sortOrder = 2),
        CategoryEntity(id = "cat-beach-pool", name = "Beach/Pool", iconKey = "\uD83C\uDFD6\uFE0F", sortOrder = 3),
        CategoryEntity(id = "cat-shabbos", name = "Shabbos", iconKey = "\uD83D\uDD6F\uFE0F", sortOrder = 4),
        CategoryEntity(id = "cat-t1d", name = "T1D", iconKey = "\uD83D\uDC89", sortOrder = 5),
        CategoryEntity(id = "cat-food", name = "Food", iconKey = "\uD83C\uDF7D\uFE0F", sortOrder = 6),
        CategoryEntity(id = "cat-winter", name = "Winter", iconKey = "\uD83E\uDDE5", sortOrder = 7),
        CategoryEntity(id = "cat-airplane", name = "Airplane", iconKey = "\u2708\uFE0F", sortOrder = 8),
    )

    val familyMembers = listOf(
        FamilyMemberEntity(id = "adult-1", name = "Adult 1", avatar = "A1", iconKey = "person", sortOrder = 0),
        FamilyMemberEntity(id = "adult-2", name = "Adult 2", avatar = "A2", iconKey = "person", sortOrder = 1),
        FamilyMemberEntity(id = "gen-adults", name = "Adult General", avatar = "AG", iconKey = "group", sortOrder = 2),
        FamilyMemberEntity(id = "gen-kids", name = "Kids General", avatar = "KG", iconKey = "family", sortOrder = 3),
    )

    private fun items(memberId: String, pairs: List<Pair<String, String>>): List<PackingItemEntity> =
        pairs.mapIndexed { i, (name, category) ->
            PackingItemEntity(
                id = "$memberId-$i-${name.lowercase().replace(" ", "-").replace("/", "-")}",
                name = name,
                category = category,
                memberId = memberId,
                sortOrder = i
            )
        }

    val packingItems: List<PackingItemEntity> = listOf(
        items("adult-1", listOf(
            "Shirts" to "Clothing",
            "Pants/shorts" to "Clothing",
            "Underwear" to "Clothing",
            "Socks" to "Clothing",
            "Pajamas" to "Clothing",
            "Sweatshirt" to "Clothing",
            "Toothbrush" to "Toiletries",
            "Toothpaste" to "Toiletries",
            "Deodorant" to "Toiletries",
        )),
        items("adult-2", listOf(
            "Shirts" to "Clothing",
            "Pants/shorts" to "Clothing",
            "Underwear" to "Clothing",
            "Socks" to "Clothing",
            "Pajamas" to "Clothing",
            "Sweatshirt" to "Clothing",
            "Toothbrush" to "Toiletries",
            "Toothpaste" to "Toiletries",
            "Deodorant" to "Toiletries",
        )),
        items("gen-adults", listOf(
            "Phone chargers" to "Misc",
            "Laptop" to "Misc",
            "Sunscreen" to "Beach/Pool",
            "First aid kit" to "Misc",
        )),
        items("gen-kids", listOf(
            "Toys" to "Misc",
            "Snacks" to "Food",
            "Wipes" to "Toiletries",
            "Stroller" to "Misc",
        )),
    ).flatten()

    val morningItems = listOf(
        MorningItemEntity(id = "morning-1-pack-lunches", name = "Pack lunches", sortOrder = 0),
        MorningItemEntity(id = "morning-2-water-bottles", name = "Water bottles", sortOrder = 1),
        MorningItemEntity(id = "morning-3-empty-fridge", name = "Empty fridge", sortOrder = 2),
        MorningItemEntity(id = "morning-4-toiletry-bag", name = "Toiletry bag", sortOrder = 3),
    )
}
