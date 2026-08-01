package com.blackink.app.core

/**
 * A lightweight, allocation-free result type used across the domain and data layers.
 *
 * We deliberately avoid throwing exceptions across layer boundaries. Repositories and
 * use cases return [DataResult] so the presentation layer can render errors as UI state
 * rather than crashing.
 */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Failure(val error: AppError) : DataResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.data
}

/**
 * Transforms a [DataResult.Success] value, propagating [DataResult.Failure] unchanged.
 *
 * Declared as an `inline` extension (rather than an interface member) because `inline`
 * is prohibited on virtual members; keeping it inline lets callers invoke suspend
 * functions inside [transform].
 */
inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(data))
    is DataResult.Failure -> this
}

inline fun <T> DataResult<T>.onSuccess(block: (T) -> Unit): DataResult<T> {
    if (this is DataResult.Success) block(data)
    return this
}

inline fun <T> DataResult<T>.onFailure(block: (AppError) -> Unit): DataResult<T> {
    if (this is DataResult.Failure) block(error)
    return this
}

/** Wraps a suspending block, converting thrown exceptions into [DataResult.Failure]. */
inline fun <T> runCatchingResult(block: () -> T): DataResult<T> =
    try {
        DataResult.Success(block())
    } catch (t: Throwable) {
        DataResult.Failure(AppError.from(t))
    }
