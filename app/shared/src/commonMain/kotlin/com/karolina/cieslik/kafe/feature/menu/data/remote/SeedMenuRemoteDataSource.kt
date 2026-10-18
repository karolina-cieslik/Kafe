package com.karolina.cieslik.kafe.feature.menu.data.remote

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.data.remote.SeedCafes
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Dane seed do czasu podłączenia Firebase.
 *
 * @param latency opóźnienie pobrania menu (dostępność – ćwierć tego), żeby było widać stan ładowania.
 */
class SeedMenuRemoteDataSource(
    private val latency: Duration = 800.milliseconds,
) : MenuRemoteDataSource {

    override suspend fun fetchMenu(): Result<MenuDto, DataError.Remote> {
        delay(latency)
        return Result.Success(SeedMenu.menu)
    }

    override suspend fun fetchAvailability(cafeId: String): Result<Map<String, Boolean>, DataError.Remote> {
        delay(latency / 4)
        return if (SeedCafes.cafes.any { it.id == cafeId }) {
            Result.Success(SeedMenu.availability[cafeId].orEmpty())
        } else {
            Result.Failure(DataError.Remote.NOT_FOUND)
        }
    }
}

internal object SeedMenu {

    private const val CLASSIC = "classic"
    private const val POUR_OVER = "pour-over"
    private const val SEASONAL = "seasonal"
    private const val SNACKS = "snacks"
    private const val TEA = "tea"

    val menu = MenuDto(
        categories = listOf(
            // Celowo nie alfabetycznie – kolejność ustala sieć.
            CategoryDto(id = SNACKS, name = "Przekąski", sortOrder = 4),
            CategoryDto(id = CLASSIC, name = "Kawy klasyczne", sortOrder = 1),
            CategoryDto(id = SEASONAL, name = "Sezonowe", sortOrder = 3),
            CategoryDto(id = POUR_OVER, name = "Przelewy", sortOrder = 2),
            // Kategoria bez produktów – ma się nie wyświetlić.
            CategoryDto(id = TEA, name = "Herbaty", sortOrder = 5),
        ),
        products = listOf(
            product("espresso", CLASSIC, "Espresso", "Podwójne, z ziaren z Etiopii", 1, size("S", 40, 900)),
            product(
                "cappuccino", CLASSIC, "Cappuccino", "Z mleczną pianką", 2,
                size("S", 250, 1300), size("M", 350, 1500), size("L", 450, 1700),
            ),
            product(
                "latte", CLASSIC, "Caffè latte", "Delikatne, dużo mleka", 3,
                size("S", 250, 1400), size("M", 350, 1600), size("L", 450, 1800),
            ),
            product("flat-white", CLASSIC, "Flat white", "Mocniejsze niż latte", 4, size("S", 200, 1500), size("M", 300, 1700)),
            product(
                "americano", CLASSIC, "Americano", "Espresso z gorącą wodą", 5,
                size("S", 250, 1100), size("M", 350, 1300), size("L", 450, 1500),
            ),
            product("v60", POUR_OVER, "V60", "Przelew z drippera, owocowy", 1, size("M", 300, 1800)),
            product("chemex", POUR_OVER, "Chemex", "Czysty, herbaciany profil", 2, size("L", 500, 2600)),
            product("aeropress", POUR_OVER, "Aeropress", "Intensywny i słodki", 3, size("S", 250, 1700)),
            product("cold-brew", POUR_OVER, "Cold brew", "Parzone na zimno 16 godzin", 4, size("M", 350, 1600), size("L", 450, 1800)),
            product(
                "pumpkin-latte", SEASONAL, "Latte dyniowe", "Z syropem dyniowym i cynamonem", 1,
                size("S", 250, 1700), size("M", 350, 1900), size("L", 450, 2100),
            ),
            // Produkt poza sezonem – nieaktywny, nie jest pokazywany.
            product("lavender-lemonade", SEASONAL, "Lemoniada lawendowa", "Orzeźwiająca, letnia", 2, size("M", 400, 1400), active = false),
            product("croissant", SNACKS, "Croissant maślany", "Pieczony na miejscu", 1, size("S", null, 950)),
            product("cheesecake", SNACKS, "Sernik baskijski", "Kremowy, z karmelizowaną skórką", 2, size("S", null, 1600)),
            product("oat-cookie", SNACKS, "Ciastko owsiane", null, 3, size("S", null, 700)),
        ),
    )

    /** Dostępność per lokal; klucze = id lokali z SeedCafes. Kraków ma wszystko, pozostałe lokale mają braki. */
    val availability: Map<String, Map<String, Boolean>> = mapOf(
        SeedCafes.KRAKOW to emptyMap(),
        SeedCafes.WARSZAWA to mapOf("cold-brew" to false),
        SeedCafes.GDANSK to mapOf("chemex" to false, "cheesecake" to false, "flat-white" to false),
    )

    private fun product(
        id: String,
        categoryId: String,
        name: String,
        description: String?,
        sortOrder: Int,
        vararg sizes: ProductSizeDto,
        active: Boolean = true,
    ) = ProductDto(
        id = id,
        categoryId = categoryId,
        name = name,
        description = description,
        imageUrl = null,
        sizes = sizes.toList(),
        sortOrder = sortOrder,
        active = active,
    )

    private fun size(code: String, volumeMl: Int?, priceGrosze: Long) =
        ProductSizeDto(code = code, volumeMl = volumeMl, priceGrosze = priceGrosze)
}
