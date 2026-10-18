package com.karolina.cieslik.kafe.core.domain

/**
 * Wspólny typ bazowy błędów zwracanych w [Result].
 *
 * Celowo nie jest `sealed` – feature'y mogą definiować własne błędy (np. walidacji) poza modułem core.
 */
interface Error
