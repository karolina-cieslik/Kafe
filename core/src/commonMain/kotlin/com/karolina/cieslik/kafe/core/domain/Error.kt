package com.karolina.cieslik.kafe.core.domain

// KAFE-2: Wspólny typ bazowy dla błędów domenowych zwracanych w Result.
// Celowo nie sealed – feature'y mogą definiować własne błędy (np. walidacji) poza modułem core.
interface Error
