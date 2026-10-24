plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.detekt) apply false
}

/**
 * Wspólna konfiguracja detekta dla modułów, które go stosują.
 *
 * Analizujemy cały katalog `src` (wszystkie source sety KMP, łącznie z iOS i testami) bez rozwiązywania typów –
 * domyślne źródła detekta (`src/main/kotlin`) w modułach KMP nie istnieją, więc zadanie `detekt` byłoby puste.
 * Reguły to domyślny zestaw z nadpisaniami w `config/detekt/detekt.yml`; zastane naruszenia są w `detekt-baseline.xml`
 * modułu, a nowe łamią build.
 */
subprojects {
    pluginManager.withPlugin("dev.detekt") {
        extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            source.setFrom("src")
            baseline = file("detekt-baseline.xml")
        }
    }
}
