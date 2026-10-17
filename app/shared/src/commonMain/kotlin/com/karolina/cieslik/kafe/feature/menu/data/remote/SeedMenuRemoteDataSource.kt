package com.karolina.cieslik.kafe.feature.menu.data.remote

import com.karolina.cieslik.kafe.core.domain.DataError
import com.karolina.cieslik.kafe.core.domain.Result
import com.karolina.cieslik.kafe.feature.cafe.data.remote.SeedCafes
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// KAFE-6: Dane seed do czasu podłączenia Firebase. Opóźnienie symuluje sieć, żeby było widać stan ładowania.
class SeedMenuRemoteDataSource(
    private val latency: Duration = 800.milliseconds,
) : MenuRemoteDataSource {

    override suspend fun fetchMenu(): Result<MenuDto, DataError.Remote> {
        delay(latency)
        return Result.Success(SeedMenu.menu)
    }

    override suspend fun fetchAvailability(cafeId: String): Result<AvailabilityDto, DataError.Remote> {
        delay(latency / 4)
        return if (SeedCafes.cafes.any { it.id == cafeId }) {
            Result.Success(SeedMenu.availability[cafeId] ?: AvailabilityDto())
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

    // KAFE-10: decyzje Karoliny – mleko jednokrotne, domyślnie krowie bez dopłaty; shoty 0–4; syrop opcjonalny.
    const val MILK = "milk"
    const val EXTRA_SHOTS = "extra-shots"
    const val SYRUP = "syrup"

    private val modifierGroups = listOf(
        ModifierGroupDto(
            id = MILK,
            name = "Mleko",
            type = "SINGLE",
            required = true,
            defaultOptionId = "milk-cow",
            options = listOf(
                option("milk-cow", "Krowie", 0, 1),
                option("milk-lactose-free", "Bez laktozy", 100, 2),
                option("milk-oat", "Owsiane", 200, 3),
                option("milk-soy", "Sojowe", 200, 4),
                option("milk-almond", "Migdałowe", 250, 5),
            ),
        ),
        ModifierGroupDto(
            id = EXTRA_SHOTS,
            name = "Dodatkowe espresso",
            type = "QUANTITY",
            maxQuantity = 4,
            options = listOf(option("extra-shot", "Shot espresso", 300, 1)),
        ),
        ModifierGroupDto(
            id = SYRUP,
            name = "Syrop",
            type = "SINGLE",
            options = listOf(
                option("syrup-vanilla", "Waniliowy", 200, 1),
                option("syrup-caramel", "Karmelowy", 200, 2),
                option("syrup-hazelnut", "Orzechowy", 200, 3),
            ),
        ),
    )

    private val milkDrink = listOf(MILK, EXTRA_SHOTS, SYRUP)

    val menu = MenuDto(
        categories = listOf(
            // Celowo nie alfabetycznie – kolejność ustala sieć (AC 3).
            CategoryDto(id = SNACKS, name = "Przekąski", sortOrder = 4),
            CategoryDto(id = CLASSIC, name = "Kawy klasyczne", sortOrder = 1),
            CategoryDto(id = SEASONAL, name = "Sezonowe", sortOrder = 3),
            CategoryDto(id = POUR_OVER, name = "Przelewy", sortOrder = 2),
            // Kategoria bez produktów – ma się nie wyświetlić (AC 4).
            CategoryDto(id = TEA, name = "Herbaty", sortOrder = 5),
        ),
        products = listOf(
            product(
                "espresso", CLASSIC, "Espresso", "Podwójne, z ziaren z Etiopii", 1, size("S", 40, 900),
                modifiers = listOf(EXTRA_SHOTS),
            ),
            product(
                "cappuccino", CLASSIC, "Cappuccino", "Z mleczną pianką", 2,
                size("S", 250, 1300), size("M", 350, 1500), size("L", 450, 1700),
                defaultSize = "M", modifiers = milkDrink,
            ),
            product(
                "latte", CLASSIC, "Caffè latte", "Delikatne, dużo mleka", 3,
                size("S", 250, 1400), size("M", 350, 1600), size("L", 450, 1800),
                defaultSize = "M", modifiers = milkDrink,
            ),
            product(
                "flat-white", CLASSIC, "Flat white", "Mocniejsze niż latte", 4, size("S", 200, 1500), size("M", 300, 1700),
                defaultSize = "S", modifiers = milkDrink,
            ),
            product(
                "americano", CLASSIC, "Americano", "Espresso z gorącą wodą", 5,
                size("S", 250, 1100), size("M", 350, 1300), size("L", 450, 1500),
                defaultSize = "M", modifiers = listOf(EXTRA_SHOTS, SYRUP),
            ),
            product("v60", POUR_OVER, "V60", "Przelew z drippera, owocowy", 1, size("M", 300, 1800)),
            product("chemex", POUR_OVER, "Chemex", "Czysty, herbaciany profil", 2, size("L", 500, 2600)),
            product("aeropress", POUR_OVER, "Aeropress", "Intensywny i słodki", 3, size("S", 250, 1700)),
            product(
                "cold-brew", POUR_OVER, "Cold brew", "Parzone na zimno 16 godzin", 4, size("M", 350, 1600), size("L", 450, 1800),
                defaultSize = "M", modifiers = listOf(SYRUP),
            ),
            product(
                "pumpkin-latte", SEASONAL, "Latte dyniowe", "Z syropem dyniowym i cynamonem", 1,
                size("S", 250, 1700), size("M", 350, 1900), size("L", 450, 2100),
                defaultSize = "M", modifiers = listOf(MILK, EXTRA_SHOTS),
            ),
            // Produkt poza sezonem – nieaktywny, nie jest pokazywany (AC 5).
            product("lavender-lemonade", SEASONAL, "Lemoniada lawendowa", "Orzeźwiająca, letnia", 2, size("M", 400, 1400), active = false),
            product("croissant", SNACKS, "Croissant maślany", "Pieczony na miejscu", 1, size("S", null, 950)),
            product("cheesecake", SNACKS, "Sernik baskijski", "Kremowy, z karmelizowaną skórką", 2, size("S", null, 1600)),
            product("oat-cookie", SNACKS, "Ciastko owsiane", null, 3, size("S", null, 700)),
        ),
        modifierGroups = modifierGroups,
    )

    // Klucze = id lokali z SeedCafes. Kraków ma wszystko, pozostałe lokale mają braki.
    val availability: Map<String, AvailabilityDto> = mapOf(
        SeedCafes.KRAKOW to AvailabilityDto(),
        SeedCafes.WARSZAWA to AvailabilityDto(
            products = mapOf("cold-brew" to false),
            // Latte bez dużego kubka; dyniowe bez obu mniejszych – domyślne M niedostępne → wybór L.
            sizes = mapOf("latte" to mapOf("L" to false), "pumpkin-latte" to mapOf("S" to false, "M" to false)),
            modifierOptions = mapOf("milk-almond" to false),
        ),
        SeedCafes.GDANSK to AvailabilityDto(
            products = mapOf("chemex" to false, "cheesecake" to false, "flat-white" to false),
            // Americano: domyślne M niedostępne → wybór S. Cappuccino: wszystkie rozmiary niedostępne → produkt niedostępny.
            sizes = mapOf(
                "americano" to mapOf("M" to false),
                "cappuccino" to mapOf("S" to false, "M" to false, "L" to false),
            ),
            // Domyślne mleko krowie niedostępne → wybór pierwszego dostępnego (bez laktozy).
            modifierOptions = mapOf("milk-cow" to false, "syrup-hazelnut" to false),
        ),
    )

    private fun product(
        id: String,
        categoryId: String,
        name: String,
        description: String?,
        sortOrder: Int,
        vararg sizes: ProductSizeDto,
        active: Boolean = true,
        defaultSize: String? = null,
        modifiers: List<String> = emptyList(),
    ) = ProductDto(
        id = id,
        categoryId = categoryId,
        name = name,
        description = description,
        imageUrl = null,
        sizes = sizes.toList(),
        sortOrder = sortOrder,
        active = active,
        defaultSize = defaultSize,
        modifierGroupIds = modifiers,
    )

    private fun size(code: String, volumeMl: Int?, priceGrosze: Long) =
        ProductSizeDto(code = code, volumeMl = volumeMl, priceGrosze = priceGrosze)

    private fun option(id: String, name: String, surchargeGrosze: Long, sortOrder: Int) =
        ModifierOptionDto(id = id, name = name, surchargeGrosze = surchargeGrosze, sortOrder = sortOrder)
}
