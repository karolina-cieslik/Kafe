package com.karolina.cieslik.kafe.core.domain

/** Jawny wynik operacji z typowanym błędem – zamiast rzucania wyjątków przez warstwy. */
sealed interface Result<out D, out E : Error> {
    /** Operacja się udała; [data] to jej wynik. */
    data class Success<out D>(val data: D) : Result<D, Nothing>
    /** Operacja się nie udała; [error] mówi dlaczego. */
    data class Failure<out E : Error>(val error: E) : Result<Nothing, E>
}

/** Wynik operacji, która niczego nie zwraca – interesuje nas tylko sukces albo błąd. */
typealias EmptyResult<E> = Result<Unit, E>

/** Przekształca dane sukcesu przez [transform]; błąd przechodzi bez zmian. */
inline fun <T, E : Error, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Failure -> this
}

/** Odrzuca dane sukcesu i zostawia sam fakt powodzenia albo błąd. */
fun <T, E : Error> Result<T, E>.asEmptyResult(): EmptyResult<E> = map { }

/** Wykonuje [action] na danych, gdy wynik jest sukcesem; zwraca ten sam wynik. */
inline fun <T, E : Error> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    if (this is Result.Success) action(data)
    return this
}

/** Wykonuje [action] na błędzie, gdy wynik jest porażką; zwraca ten sam wynik. */
inline fun <T, E : Error> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Failure) action(error)
    return this
}
