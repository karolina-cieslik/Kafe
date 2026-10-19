package com.karolina.cieslik.kafe.feature.cafe.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.karolina.cieslik.kafe.core.presentation.designsystem.theme.KafeTheme

/** Dane fake do previewów wyboru lokalu. */
internal object CafePickerPreviewData {
    private val cafes = listOf(
        CafeUi("rynek", "Kafe Rynek", "Rynek Główny 12, Kraków", "Dziś 7:00–21:00", isOpenNow = true, isSelected = true),
        CafeUi("mokotow", "Kafe Mokotów", "ul. Puławska 45, Warszawa", "Dziś 8:00–20:00", isOpenNow = true, isSelected = false),
        CafeUi("oliwa", "Kafe Oliwa", "ul. Grunwaldzka 410, Gdańsk", "Otwieramy jutro o 8:00", isOpenNow = false, isSelected = false),
    )

    val content = CafePickerUiState.Content(cafes = cafes, isFirstLaunch = false)
    val firstLaunch = CafePickerUiState.Content(
        cafes = cafes.map { it.copy(isSelected = false) },
        isFirstLaunch = true,
    )
}

@Composable
private fun CafePickerPreview(state: CafePickerUiState, canNavigateBack: Boolean = true) {
    KafeTheme { CafePickerScreen(state = state, onAction = {}, canNavigateBack = canNavigateBack) }
}

@PreviewLightDark
@Composable
private fun CafePickerContentPreview() = CafePickerPreview(CafePickerPreviewData.content)

@PreviewLightDark
@Composable
private fun CafePickerFirstLaunchPreview() = CafePickerPreview(CafePickerPreviewData.firstLaunch, canNavigateBack = false)

@PreviewLightDark
@Composable
private fun CafePickerFirstLaunchLoadingPreview() = CafePickerPreview(CafePickerUiState.Loading, canNavigateBack = false)

@PreviewLightDark
@Composable
private fun CafePickerFirstLaunchErrorPreview() = CafePickerPreview(
    CafePickerUiState.Error("Nie udało się pobrać listy kawiarni. Sprawdź połączenie z internetem."),
    canNavigateBack = false,
)

@Preview
@Composable
private fun CafePickerEmptyPreview() =
    CafePickerPreview(CafePickerUiState.Content(cafes = emptyList(), isFirstLaunch = false))

@Preview
@Composable
private fun CafePickerLoadingPreview() = CafePickerPreview(CafePickerUiState.Loading)

@Preview
@Composable
private fun CafePickerErrorPreview() =
    CafePickerPreview(CafePickerUiState.Error("Nie udało się pobrać listy kawiarni."))
